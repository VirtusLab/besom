import besom.*
import besom.api.gcp

enum GCPService(val name: String):
  case CloudBuild extends GCPService("cloudbuild.googleapis.com")
  case CloudFunctions extends GCPService("cloudfunctions.googleapis.com")
  case ArtifactRegistry extends GCPService("artifactregistry.googleapis.com")
  case CloudLogging extends GCPService("logging.googleapis.com")
  case CloudPubSub extends GCPService("pubsub.googleapis.com")

@main def main = Pulumi.run {
  // Enable GCP service(s) for the current project
  val enableServices: Map[GCPService, Output[gcp.projects.Service]] =
    GCPService.values
      .map(api => api -> projectService(api))
      .toMap

  val bucket = gcp.storage.Bucket(
    name = "my-bucket",
    gcp.storage.BucketArgs(
      location = "US",
      forceDestroy = true
    )
  )

  // Provide the source code for the function as a string
  val functionSourceCode =
    """
      |const functions = require('@google-cloud/functions-framework');
      |
      |functions.http('helloHttp', (req, res) => {
      |  res.send(`Hello ${req.query.name || req.body.name || 'World'}!`);
      |});
      |""".stripMargin

  // Provide dependencies for the function as a string
  val dependencies =
    """
      |{
      |  "dependencies": {
      |    "@google-cloud/functions-framework": "^3.0.0"
      |  }
      |}
      |""".stripMargin

  val archive = gcp.storage.BucketObject(
    name = "archive",
    gcp.storage.BucketObjectArgs(
      name = "index.zip",
      bucket = bucket.name,
      source = Archive.AssetArchive(
        Map(
          "index.js" -> Asset.StringAsset(functionSourceCode),
          "package.json" -> Asset.StringAsset(dependencies)
        )
      )
    )
  )

  val repositoryIamMember = gcp.artifactregistry.RepositoryIamMember(
    name = "repository-iam-member",
    gcp.artifactregistry.RepositoryIamMemberArgs(
      repository = "gcf-artifacts",
      role = "roles/artifactregistry.reader",
      member = "allUsers"
    ),
    opts = opts(dependsOn = enableServices(GCPService.ArtifactRegistry))
  )

  val function = gcp.cloudfunctions.Function(
    name = "function",
    gcp.cloudfunctions.FunctionArgs(
      description = "My function",
      runtime = "nodejs20",
      availableMemoryMb = 128,
      sourceArchiveBucket = bucket.name,
      sourceArchiveObject = archive.name,
      triggerHttp = true,
      entryPoint = "helloHttp"
    ),
    opts = opts(dependsOn =
      List(
        repositoryIamMember,
        enableServices(GCPService.CloudBuild),
        enableServices(GCPService.CloudFunctions),
        enableServices(GCPService.ArtifactRegistry),
        enableServices(GCPService.CloudLogging),
        enableServices(GCPService.CloudPubSub)
      )
    )
  )

// IAM entry for all users to invoke the function
  val invoker = gcp.cloudfunctions.FunctionIamMember(
    name = "invoker",
    gcp.cloudfunctions.FunctionIamMemberArgs(
      project = function.project,
      region = function.region,
      cloudFunction = function.name,
      role = "roles/cloudfunctions.invoker",
      member = "allUsers"
    )
  )

  Stack(invoker).exports(
    url = function.httpsTriggerUrl
  )
}

private def projectService(api: GCPService)(using Context): Output[gcp.projects.Service] =
  gcp.projects.Service(
    name = s"enable-${api.name.replace(".", "-")}",
    gcp.projects.ServiceArgs(
      service = api.name,
      // if true - at every destroy this will disable the dependent services for the whole project
      disableDependentServices = true,
      // if true - at every destroy this will disable the service for the whole project
      disableOnDestroy = true
    )
  )
