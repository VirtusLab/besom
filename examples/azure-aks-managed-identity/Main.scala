import besom.*
import besom.api.azurenative
import besom.api.tls

@main def main = Pulumi.run {
  // create a resource group to hold all the resources
  val resourceGroup = azurenative.resources.ResourceGroup("resourceGroup")

  // create a private key to use for the cluster's ssh key
  val privateKey = tls.PrivateKey(
    name = "privateKey",
    tls.PrivateKeyArgs(
      algorithm = "RSA",
      rsaBits = 4096
    )
  )

  // create a user assigned identity to use for the cluster
  val identity = azurenative.managedidentity.UserAssignedIdentity(
    name = "identity",
    azurenative.managedidentity.UserAssignedIdentityArgs(resourceGroupName = resourceGroup.name)
  )

  // create the cluster
  val cluster = azurenative.containerservice.ManagedCluster(
    name = "cluster",
    azurenative.containerservice.ManagedClusterArgs(
      resourceGroupName = resourceGroup.name,
      // replace "dns-prefix" with your desired DNS prefix
      dnsPrefix = "dns-prefix",
      enableRBAC = true,
      identity = azurenative.containerservice.inputs.ManagedClusterIdentityArgs(
        `type` = azurenative.containerservice.enums.ResourceIdentityType.UserAssigned,
        userAssignedIdentities = List(identity.id)
      ),
      agentPoolProfiles = List(
        azurenative.containerservice.inputs.ManagedClusterAgentPoolProfileArgs(
          count = 1,
          vmSize = "Standard_A2_v2",
          mode = azurenative.containerservice.enums.AgentPoolMode.System,
          name = "agentpool",
          osType = azurenative.containerservice.enums.OsType.Linux,
          osDiskSizeGB = 30,
          `type` = azurenative.containerservice.enums.AgentPoolType.VirtualMachineScaleSets
        )
      ),
      linuxProfile = azurenative.containerservice.inputs.ContainerServiceLinuxProfileArgs(
        adminUsername = "aksuser",
        ssh = azurenative.containerservice.inputs.ContainerServiceSshConfigurationArgs(
          publicKeys = List(
            azurenative.containerservice.inputs.ContainerServiceSshPublicKeyArgs(
              keyData = privateKey.publicKeyOpenssh
            )
          )
        )
      )
    )
  )

  // retrieve the admin credentials which contain the kubeconfig
  val adminCredentials = azurenative.containerservice
    .listManagedClusterUserCredentials(
      azurenative.containerservice.ListManagedClusterUserCredentialsArgs(
        resourceName = cluster.name,
        resourceGroupName = resourceGroup.name
      )
    )

  // grant the 'contributor' role to the identity on the resource group
  val assignment = azurenative.authorization.RoleAssignment(
    name = "roleAssignment",
    azurenative.authorization.RoleAssignmentArgs(
      principalId = identity.principalId,
      principalType = azurenative.authorization.enums.PrincipalType.ServicePrincipal,
      roleDefinitionId = "/providers/Microsoft.Authorization/roleDefinitions/b24988ac-6180-42a0-ab88-20f7382dd24c",
      scope = resourceGroup.id
    )
  )

  val kubeconfig = adminCredentials.kubeconfigs
    .flatMap(_.head.value)

  // export the kubeconfig
  Stack(assignment).exports(
    kubeconfig = kubeconfig.map(base64.decode)
  )
}

object base64:
  def decode(v: String): String =
    new String(java.util.Base64.getDecoder.decode(v.getBytes("UTF-8")), java.nio.charset.StandardCharsets.UTF_8)
