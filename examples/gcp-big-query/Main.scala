import besom.*
import besom.api.gcp
import besom.json.*

@main def main = Pulumi.run {
  val projectId = config.requireString("gcp:project")

  val dataset = gcp.bigquery.Dataset(
    name = "my-dataset",
    gcp.bigquery.DatasetArgs(
      datasetId = "my_dataset"
    )
  )

  // Create the source table in BigQuery
  val sourceTable = gcp.bigquery.Table(
    "source-table",
    gcp.bigquery.TableArgs(
      deletionProtection = false,
      datasetId = dataset.datasetId,
      tableId = "source_table",
      schema = json"""[
             {
               "name": "name",
               "type": "STRING",
               "mode": "NULLABLE",
               "description": "Person name"
             },
             {
               "name": "age",
               "type": "INTEGER",
               "mode": "NULLABLE",
               "description": "Person age"
             }
           ]""".map(_.prettyPrint)
    )
  )

  // Data to be inserted into BigQuery table
  // Convert data to newline-delimited JSON as required by BigQuery
  val data = Output
    .sequence(
      List(
        json"""{ "name": "Ken", "age": 25 }""",
        json"""{ "name": "Alice", "age": 30 }""",
        json"""{ "name": "Bob", "age": 35 }"""
      )
    )
    .map(_.map(_.compactPrint).mkString("\n"))

  // Create a GCS bucket to store the data
  val bucket = gcp.storage.Bucket(
    name = "my-bucket",
    gcp.storage.BucketArgs(
      location = "US"
    )
  )

// Create a GCS object to store the data
  val bucketObject = gcp.storage.BucketObject(
    name = "my-object",
    gcp.storage.BucketObjectArgs(
      contentType = "application/json",
      bucket = bucket.name,
      content = data
    )
  )

  // Create a BigQuery job to load the data from GCS into the BigQuery table
  val loadJob = gcp.bigquery.Job(
    name = "load-data-job",
    gcp.bigquery.JobArgs(
      jobId = s"load_data_job",
      load = gcp.bigquery.inputs.JobLoadArgs(
        sourceUris = List(p"gs://${bucketObject.bucket}/${bucketObject.name}"),
        destinationTable = gcp.bigquery.inputs.JobLoadDestinationTableArgs(
          projectId = projectId,
          datasetId = dataset.datasetId,
          tableId = sourceTable.tableId
        ),
        sourceFormat = "NEWLINE_DELIMITED_JSON",
        writeDisposition = "WRITE_EMPTY"
      )
    )
  )

  val destinationTable = gcp.bigquery.Table(
    name = "destination-table",
    gcp.bigquery.TableArgs(
      deletionProtection = false,
      datasetId = dataset.datasetId,
      tableId = "destination_table",
      schema = json"""[
             {
               "name": "name",
               "type": "STRING",
               "mode": "NULLABLE",
               "description": "Person name"
             }
           ]""".map(_.prettyPrint)
    )
  )

  // Create a BigQuery job to execute the query and write the results to the destination table
  val queryJob = gcp.bigquery.Job(
    name = "query-data-job",
    gcp.bigquery.JobArgs(
      jobId = s"query_data_job",
      query = gcp.bigquery.inputs.JobQueryArgs(
        // Define the SQL query to filter data and save to another table
        query = p"SELECT name FROM $projectId.${dataset.datasetId}.${sourceTable.tableId} WHERE age > 30",
        destinationTable = gcp.bigquery.inputs.JobQueryDestinationTableArgs(
          projectId = projectId,
          datasetId = dataset.datasetId,
          tableId = destinationTable.tableId
        ),
        writeDisposition = "WRITE_TRUNCATE"
      )
    )
  )

  Stack.exports(
    loadJobId = loadJob.id,
    queryJobId = queryJob.id,
    sourceTableId = sourceTable.tableId,
    destinationTableId = destinationTable.tableId
  )
}
