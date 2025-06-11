import besom.*
import besom.api.azurenative
import besom.json.*

@main def main = Pulumi.run {
  // Create an Azure Resource Group
  val resourceGroup = azurenative.resources.ResourceGroup("logicappdemo-rg")

  // Create an Azure resource (Storage Account)
  val storageAccount = azurenative.storage.StorageAccount(
    name = "logicappdemosa",
    azurenative.storage.StorageAccountArgs(
      resourceGroupName = resourceGroup.name,
      sku = azurenative.storage.inputs.SkuArgs(
        name = azurenative.storage.enums.SkuName.Standard_LRS
      ),
      kind = azurenative.storage.enums.Kind.StorageV2
    )
  )

  // Cosmos DB Account
  val cosmosdbAccount = azurenative.cosmosdb.DatabaseAccount(
    "logicappdemo-cdb",
    azurenative.cosmosdb.DatabaseAccountArgs(
      resourceGroupName = resourceGroup.name,
      databaseAccountOfferType = azurenative.cosmosdb.enums.DatabaseAccountOfferType.Standard,
      locations = List(
        azurenative.cosmosdb.inputs.LocationArgs(
          locationName = resourceGroup.location,
          failoverPriority = 0
        )
      ),
      consistencyPolicy = azurenative.cosmosdb.inputs.ConsistencyPolicyArgs(
        defaultConsistencyLevel = azurenative.cosmosdb.enums.DefaultConsistencyLevel.Session
      )
    )
  )

  // Cosmos DB Database
  val db = azurenative.cosmosdb.SqlResourceSqlDatabase(
    "sqldb",
    azurenative.cosmosdb.SqlResourceSqlDatabaseArgs(
      resourceGroupName = resourceGroup.name,
      accountName = cosmosdbAccount.name,
      resource = azurenative.cosmosdb.inputs.SqlDatabaseResourceArgs(
        id = "sqldb"
      )
    )
  )

// Cosmos DB SQL Container
  val dbContainer = azurenative.cosmosdb.SqlResourceSqlContainer(
    "container",
    azurenative.cosmosdb.SqlResourceSqlContainerArgs(
      resourceGroupName = resourceGroup.name,
      accountName = cosmosdbAccount.name,
      databaseName = db.name,
      resource = azurenative.cosmosdb.inputs.SqlContainerResourceArgs(
        id = "container",
        partitionKey = azurenative.cosmosdb.inputs.ContainerPartitionKeyArgs(
          paths = List("/myPartitionKey"),
          kind = "Hash"
        )
      )
    )
  )

  val accountKeys = azurenative.cosmosdb.listDatabaseAccountKeys(
    azurenative.cosmosdb.ListDatabaseAccountKeysArgs(
      accountName = cosmosdbAccount.name,
      resourceGroupName = resourceGroup.name
    )
  )
  val clientConfig = azurenative.authorization.getClientConfig()

  val apiId =
    p"/subscriptions/${clientConfig.subscriptionId}/providers/Microsoft.Web/locations/${resourceGroup.location}/managedApis/documentdb"

// API Connection to be used in a Logic App
  val connection = azurenative.web.Connection(
    "cosmosdbConnection",
    azurenative.web.ConnectionArgs(
      resourceGroupName = resourceGroup.name,
      properties = azurenative.web.inputs.ApiConnectionDefinitionPropertiesArgs(
        displayName = "cosmosdb_connection",
        api = azurenative.web.inputs.ApiReferenceArgs(
          id = apiId
        ),
        parameterValues = Map(
          "databaseAccount" -> cosmosdbAccount.name,
          "accessKey" -> accountKeys.primaryMasterKey
        )
      )
    )
  )

  val path = p"/dbs/${db.name}/colls/${dbContainer.name}/docs"

// Logic App with an HTTP trigger and Cosmos DB action
  val workflow = azurenative.logic.Workflow(
    "httpToCosmos",
    azurenative.logic.WorkflowArgs(
      resourceGroupName = resourceGroup.name,
      definition = json"""{
        "$$schema": "https://schema.management.azure.com/providers/Microsoft.Logic/schemas/2016-06-01/workflowdefinition.json#",
        "contentVersion": "1.0.0.0",
        "parameters": {
            "$$connections": {
                "defaultValue": {},
                "type": "Object"
            }
        },
        "triggers": {
            "Receive_post": {
                "type": "Request",
                "kind": "Http",
                "inputs": {
                    "method": "POST",
                    "schema": {
                        "properties": {},
                        "type": "object"
                    }
                }
            }
        },
        "actions": {
            "write_body": {
                "type": "ApiConnection",
                "inputs": {
                    "body": {
                        "data": "@triggerBody()",
                        "id": "@utcNow()"
                    },
                    "host": {
                        "connection": {
                            "name": "@parameters('$$connections')['documentdb']['connectionId']"
                        }
                    },
                    "method": "post",
                    "path": $path
                }
            }
        }
    }""",
      parameters = Map(
        "$connections" -> azurenative.logic.inputs.WorkflowParameterArgs(
          value = json"""{
                "documentdb": {
                    "connectionId": ${connection.id},
                    "connectionName": "logicapp-cosmosdb-connection",
                    "id": $apiId
                }
            }"""
        )
      )
    )
  )

  val callbackUrls = azurenative.logic.listWorkflowTriggerCallbackUrl(
    azurenative.logic.ListWorkflowTriggerCallbackUrlArgs(
      resourceGroupName = resourceGroup.name,
      workflowName = workflow.name,
      triggerName = "Receive_post"
    )
  )

// Export the HTTP endpoint
  Stack(storageAccount).exports(
    endpoint = callbackUrls.value
  )
}
