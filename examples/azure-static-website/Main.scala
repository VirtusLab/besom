import besom.*
import besom.api.azurenative

@main def main = Pulumi.run {
  // All resources will share a resource group.
  val resourceGroupName = azurenative.resources.ResourceGroup("resourceGroup").name

  val profile = azurenative.cdn.Profile(
    name = "profile",
    azurenative.cdn.ProfileArgs(
      resourceGroupName = resourceGroupName,
      location = "global",
      sku = azurenative.cdn.inputs.SkuArgs(
        name = azurenative.cdn.enums.SkuName.Standard_AzureFrontDoor
      )
    )
  )

  val storageAccount = azurenative.storage.StorageAccount(
    name = "storageaccount",
    azurenative.storage.StorageAccountArgs(
      enableHttpsTrafficOnly = true,
      kind = azurenative.storage.enums.Kind.StorageV2,
      resourceGroupName = resourceGroupName,
      sku = azurenative.storage.inputs.SkuArgs(
        name = azurenative.storage.enums.SkuName.Standard_LRS
      )
    )
  )

  // Enable static website support
  val staticWebsite = azurenative.storage.StorageAccountStaticWebsite(
    name = "staticWebsite",
    azurenative.storage.StorageAccountStaticWebsiteArgs(
      accountName = storageAccount.name,
      resourceGroupName = resourceGroupName,
      indexDocument = "index.html",
      error404Document = "404.html"
    )
  )

  // Upload the files
  val blobs = List("index.html", "404.html").map(name =>
    azurenative.storage.Blob(
      name = NonEmptyString(name).get,
      azurenative.storage.BlobArgs(
        resourceGroupName = resourceGroupName,
        accountName = storageAccount.name,
        containerName = staticWebsite.containerName,
        source = besom.types.Asset.FileAsset(s"./wwwroot/$name"),
        contentType = "text/html"
      )
    )
  )

  val endpointOrigin = storageAccount.primaryEndpoints.web.map(_.replace("https://", "").replace("/", ""))

  val endpoint = azurenative.cdn.AfdEndpoint(
    name = "endpoint",
    azurenative.cdn.AfdEndpointArgs(
      endpointName = storageAccount.name.map(sa => s"cdn-endpnt-$sa"),
      profileName = profile.name,
      resourceGroupName = resourceGroupName,
      location = "global",
      enabledState = azurenative.cdn.enums.EnabledState.Enabled
    )
  )

  // Create an origin group for Azure Front Door
  val originGroup = azurenative.cdn.AfdOriginGroup(
    name = "originGroup",
    azurenative.cdn.AfdOriginGroupArgs(
      originGroupName = "storage-origin-group",
      profileName = profile.name,
      resourceGroupName = resourceGroupName,
      loadBalancingSettings = azurenative.cdn.inputs.LoadBalancingSettingsParametersArgs(
        sampleSize = 4,
        successfulSamplesRequired = 3,
        additionalLatencyInMilliseconds = 50
      ),
      healthProbeSettings = azurenative.cdn.inputs.HealthProbeParametersArgs(
        probePath = "/",
        probeRequestType = azurenative.cdn.enums.HealthProbeRequestType.GET,
        probeProtocol = azurenative.cdn.enums.ProbeProtocol.Https,
        probeIntervalInSeconds = 240
      )
    )
  )

  // Create an origin for the storage account
  val origin = azurenative.cdn.AfdOrigin(
    name = "origin",
    azurenative.cdn.AfdOriginArgs(
      originName = "storage-origin",
      originGroupName = originGroup.name,
      profileName = profile.name,
      resourceGroupName = resourceGroupName,
      hostName = endpointOrigin,
      httpsPort = 443,
      originHostHeader = endpointOrigin,
      priority = 1,
      weight = 1000,
      enabledState = azurenative.cdn.enums.EnabledState.Enabled
    )
  )

  // Create a route to connect the endpoint to the origin group
  val route = azurenative.cdn.Route(
    name = "route",
    azurenative.cdn.RouteArgs(
      routeName = "default-route",
      endpointName = endpoint.name,
      profileName = profile.name,
      resourceGroupName = resourceGroupName,
      originGroup = azurenative.cdn.inputs.ResourceReferenceArgs(
        id = originGroup.id
      ),
      patternsToMatch = List("/*"),
      supportedProtocols = List(azurenative.cdn.enums.AfdEndpointProtocols.Https),
      linkToDefaultDomain = azurenative.cdn.enums.LinkToDefaultDomain.Enabled,
      forwardingProtocol = azurenative.cdn.enums.ForwardingProtocol.HttpsOnly,
      httpsRedirect = azurenative.cdn.enums.HttpsRedirect.Enabled
    ),
    opts(dependsOn = origin)
  )

  Stack(Output.sequence(blobs), route).exports(
    staticEndpoint = storageAccount.primaryEndpoints.web,
    // Azure Front Door endpoint to the website.
    // Allow it some time after the deployment to get ready.
    cdnEndpoint = p"https://${endpoint.hostName}/"
  )
}
