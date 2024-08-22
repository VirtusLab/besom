import besom.*
import besom.api.azurenative

@main def main = Pulumi.run {

  // setup a resource group
  val resourceGroup = azurenative.resources.ResourceGroup("resourcegroup")

  val serverfarm = azurenative.web.AppServicePlan(
    "appServerFarm",
    azurenative.web.AppServicePlanArgs(
      kind = "app",
      resourceGroupName = resourceGroup.name,
      sku = azurenative.web.inputs.SkuDescriptionArgs(
        capacity = 1,
        family = "P1v2",
        name = "P1v2",
        size = "P1v2",
        tier = "PremiumV2"
      )
    )
  )

  // Setup backend app
  val backendApp = azurenative.web.WebApp(
    "backendApp",
    azurenative.web.WebAppArgs(
      kind = "app",
      resourceGroupName = resourceGroup.name,
      serverFarmId = serverfarm.id
    )
  )

  // Setup frontend app
  val frontendApp = azurenative.web.WebApp(
    "frontendApp",
    azurenative.web.WebAppArgs(
      kind = "app",
      resourceGroupName = resourceGroup.name,
      serverFarmId = serverfarm.id
    )
  )

  // Setup a vnet
  val virtualNetworkCIDR = config.getString("virtualNetworkCIDR") getOrElse ("10.200.0.0/16")
  val virtualNetwork = azurenative.network.VirtualNetwork(
    "virtualNetwork",
    azurenative.network.VirtualNetworkArgs(
      addressSpace = azurenative.network.inputs.AddressSpaceArgs(
        addressPrefixes = List(virtualNetworkCIDR)
      ),
      resourceGroupName = resourceGroup.name,
      virtualNetworkName = "vnet"
    ),
    opts = opts(ignoreChanges = List("subnets"))
  ) // https://github.com/pulumi/pulumi-azure-nextgen/issues/103

  // Setup private DNS zone
  val privateDnsZone = azurenative.network.PrivateZone(
    "privateDnsZone",
    azurenative.network.PrivateZoneArgs(
      location = "global",
      privateZoneName = "privatelink.azurewebsites.net",
      resourceGroupName = resourceGroup.name
    ),
    opts = opts(dependsOn = virtualNetwork)
  )

  // Setup a private subnet for backend
  val backendCIDR = config.getString("backendCIDR").getOrElse("10.200.1.0/24")
  val backendSubnet = azurenative.network.Subnet(
    "subnetForBackend",
    azurenative.network.SubnetArgs(
      addressPrefix = backendCIDR,
      privateEndpointNetworkPolicies = azurenative.network.enums.VirtualNetworkPrivateEndpointNetworkPolicies.Disabled,
      resourceGroupName = resourceGroup.name,
      virtualNetworkName = virtualNetwork.name
    )
  )

  // Private endpoint in the private subnet for backend
  val privateEndpoint = azurenative.network.PrivateEndpoint(
    "privateEndpointForBackend",
    azurenative.network.PrivateEndpointArgs(
      privateLinkServiceConnections = List(
        azurenative.network.inputs.PrivateLinkServiceConnectionArgs(
          groupIds = List("sites"),
          name = "privateEndpointLink1",
          privateLinkServiceId = backendApp.id
        )
      ),
      resourceGroupName = resourceGroup.name,
      subnet = azurenative.network.inputs.SubnetArgs(
        id = backendSubnet.id
      )
    )
  )

  // Setup a private DNS Zone for private endpoint
  val privateDNSZoneGroup = azurenative.network.PrivateDnsZoneGroup(
    "privateDnsZoneGroup",
    azurenative.network.PrivateDnsZoneGroupArgs(
      privateDnsZoneConfigs = List(
        azurenative.network.inputs.PrivateDnsZoneConfigArgs(
          name = "config1",
          privateDnsZoneId = privateDnsZone.id
        )
      ),
      privateDnsZoneGroupName = privateEndpoint.name,
      privateEndpointName = privateEndpoint.name,
      resourceGroupName = resourceGroup.name
    )
  )

  val virtualNetworkLink = azurenative.network.VirtualNetworkLink(
    "virtualNetworkLink",
    azurenative.network.VirtualNetworkLinkArgs(
      location = "global",
      privateZoneName = privateDnsZone.name,
      registrationEnabled = false,
      resourceGroupName = resourceGroup.name,
      virtualNetwork = azurenative.network.inputs.SubResourceArgs(
        id = virtualNetwork.id
      )
    )
  )

  // Now setup frontend subnet
  val frontendCIDR = config.getString("frontendCIDR").getOrElse("10.200.2.0/24")
  val frontendSubnet = azurenative.network.Subnet(
    "frontendSubnet",
    azurenative.network.SubnetArgs(
      addressPrefix = frontendCIDR,
      delegations = List(
        azurenative.network.inputs.DelegationArgs(
          name = "delegation",
          serviceName = "Microsoft.Web/serverfarms"
        )
      ),
      privateEndpointNetworkPolicies = azurenative.network.enums.VirtualNetworkPrivateEndpointNetworkPolicies.Enabled,
      resourceGroupName = resourceGroup.name,
      virtualNetworkName = virtualNetwork.name
    )
  )

  val virtualNetworkConn = azurenative.web.WebAppSwiftVirtualNetworkConnection(
    "virtualNetworkConnForFrontend",
    azurenative.web.WebAppSwiftVirtualNetworkConnectionArgs(
      name = frontendApp.name,
      resourceGroupName = resourceGroup.name,
      subnetResourceId = frontendSubnet.id
    )
  )

  Stack(virtualNetworkLink, virtualNetworkConn).exports(
    backendURL = backendApp.defaultHostName,
    frontEndURL = frontendApp.defaultHostName,
    privateEndpointURL = privateDNSZoneGroup.privateDnsZoneConfigs
      .flatMap(zoneConfigs => zoneConfigs.get.head.recordSets.map(_.head.fqdn))
  )
}
