import besom.*
import besom.api.azurenative
import besom.api.random

@main def main = Pulumi.run {

  val resourceGroup = azurenative.resources.ResourceGroup("synapse-rg")

  val storageAccount = azurenative.storage.StorageAccount(
    name = "synapsesa",
    azurenative.storage.StorageAccountArgs(
      resourceGroupName = resourceGroup.name,
      accessTier = azurenative.storage.enums.AccessTier.Hot,
      enableHttpsTrafficOnly = true,
      isHnsEnabled = true,
      kind = azurenative.storage.enums.Kind.StorageV2,
      sku = azurenative.storage.inputs.SkuArgs(
        name = azurenative.storage.enums.SkuName.Standard_RAGRS
      )
    )
  )

  val users = azurenative.storage.BlobContainer(
    name = "users",
    azurenative.storage.BlobContainerArgs(
      resourceGroupName = resourceGroup.name,
      accountName = storageAccount.name,
      publicAccess = None
    )
  )

  val dataLakeStorageAccountUrl = p"https://${storageAccount.name}.dfs.core.windows.net"

  val workspace = azurenative.synapse.Workspace(
    name = "my-workspace",
    azurenative.synapse.WorkspaceArgs(
      resourceGroupName = resourceGroup.name,
      defaultDataLakeStorage = azurenative.synapse.inputs.DataLakeStorageAccountDetailsArgs(
        accountUrl = dataLakeStorageAccountUrl,
        filesystem = "users"
      ),
      identity = azurenative.synapse.inputs.ManagedIdentityArgs(
        `type` = azurenative.synapse.enums.ResourceIdentityType.SystemAssigned
      ),
      sqlAdministratorLogin = "sqladminuser",
      sqlAdministratorLoginPassword = random.RandomPassword("workspacePwd", random.RandomPasswordArgs(length = 12)).result
    )
  )

  val firewallRule = azurenative.synapse.IpFirewallRule(
    name = "allowAll",
    azurenative.synapse.IpFirewallRuleArgs(
      resourceGroupName = resourceGroup.name,
      workspaceName = workspace.name,
      endIpAddress = "255.255.255.255",
      startIpAddress = "0.0.0.0"
    )
  )

  val subscriptionId = resourceGroup.id.map(_.split("/")(2))
  val roleDefinitionId =
    p"/subscriptions/$subscriptionId/providers/Microsoft.Authorization/roleDefinitions/ba92f5b4-2d11-453d-a403-e96b0029c9fe"

  val storageAccess = azurenative.authorization.RoleAssignment(
    name = "storageAccess",
    azurenative.authorization.RoleAssignmentArgs(
      roleAssignmentName = random.RandomUuid("roleName").result,
      scope = storageAccount.id,
      principalId = workspace.identity.principalId.getOrElse("<preview>"),
      principalType = azurenative.authorization.enums.PrincipalType.ServicePrincipal,
      roleDefinitionId = roleDefinitionId
    )
  )

  val clientConfig = azurenative.authorization.getClientConfig()

  val userAccess = azurenative.authorization.RoleAssignment(
    name = "userAccess",
    azurenative.authorization.RoleAssignmentArgs(
      roleAssignmentName = random.RandomUuid("userRoleName").result,
      scope = storageAccount.id,
      principalId = clientConfig.objectId,
      principalType = azurenative.authorization.enums.PrincipalType.User,
      roleDefinitionId = roleDefinitionId
    )
  )

  val sqlPool = azurenative.synapse.SqlPool(
    name = "SQLPOOL1",
    azurenative.synapse.SqlPoolArgs(
      resourceGroupName = resourceGroup.name,
      workspaceName = workspace.name,
      collation = "SQL_Latin1_General_CP1_CI_AS",
      createMode = azurenative.synapse.enums.CreateMode.Default,
      sku = azurenative.synapse.inputs.SkuArgs(
        name = "DW100c"
      )
    )
  )

  val sparkPool = azurenative.synapse.BigDataPool(
    name = "Spark1",
    azurenative.synapse.BigDataPoolArgs(
      resourceGroupName = resourceGroup.name,
      workspaceName = workspace.name,
      autoPause = azurenative.synapse.inputs.AutoPausePropertiesArgs(
        delayInMinutes = 15,
        enabled = true
      ),
      autoScale = azurenative.synapse.inputs.AutoScalePropertiesArgs(
        enabled = true,
        maxNodeCount = 3,
        minNodeCount = 3
      ),
      nodeCount = 3,
      nodeSize = azurenative.synapse.enums.NodeSize.Small,
      nodeSizeFamily = azurenative.synapse.enums.NodeSizeFamily.MemoryOptimized,
      sparkVersion = "3.3"
    )
  )

  Stack(users, firewallRule, storageAccess, userAccess, sqlPool, sparkPool).exports(
    sparkPoolName = sparkPool.name
  )
}
