import besom.*
import besom.api.gcp
import besom.json.*

@main def main = Pulumi.run {
  val firestoreService = gcp.projects.Service(
    s"enable-firestore-googleapis-com",
    gcp.projects.ServiceArgs(
      service = "firestore.googleapis.com",
      disableDependentServices = true,
      disableOnDestroy = true
    )
  )

  // Create a Firestore database in Native mode
  val database = gcp.firestore.Database(
    "my-database",
    gcp.firestore.DatabaseArgs(
      locationId = "us-east1",
      `type` = "FIRESTORE_NATIVE",
      concurrencyMode = "OPTIMISTIC",
      appEngineIntegrationMode = "DISABLED",
      pointInTimeRecoveryEnablement = "POINT_IN_TIME_RECOVERY_ENABLED",
      deleteProtectionState = "DELETE_PROTECTION_DISABLED",
      deletionPolicy = "DELETE"
    ),
    opts = opts(dependsOn = firestoreService)
  )

  // Add documents to the Firestore collection
  val document1 = gcp.firestore.Document(
    "doc-1",
    gcp.firestore.DocumentArgs(
      database = database.name,
      collection = "my-collection",
      documentId = "doc1",
      fields = json"""{
        "field1": { "stringValue": "value1" },
        "field2": { "integerValue": "123" }
    }""".map(_.prettyPrint)
    )
  )

  val document2 = gcp.firestore.Document(
    "doc-2",
    gcp.firestore.DocumentArgs(
      database = database.name,
      collection = "my-collection",
      documentId = "doc2",
      fields = json"""{
        "field1": { "stringValue": "value2" },
        "field2": { "integerValue": "456" }
    }""".map(_.prettyPrint)
    )
  )

  Stack(document1, document2).exports(
    databaseId = database.name
  )
}
