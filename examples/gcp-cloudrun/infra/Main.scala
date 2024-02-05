import besom.*
import besom.api.gcp
import besom.api.docker

@main def main = Pulumi.run {
  val project = config.requireString("gcp:project")
  val region  = config.requireString("gcp:region")

  // Enable GCP service(s) for the current project
  val enableServices: Map[String, Output[gcp.projects.Service]] = List(
    "run.googleapis.com"
  ).map(api =>
    api -> gcp.projects.Service(
      s"enable-${api.replace(".", "-")}",
      gcp.projects.ServiceArgs(
        project = project,
        service = api,
        /* if true - at every destroy this will disable the dependent services for the whole project */
        disableDependentServices = true,
        /* if true - at every destroy this will disable the service for the whole project */
        disableOnDestroy = true
      )
    )
  ).toMap

  val repoName      = p"gcr.io/${project}/${pulumiProject}" // will be automatically created by GCP on docker push
  val appName       = "app"
  val imageFullName = p"${repoName}/${appName}:latest"

  // Build a Docker image from our sample Scala app and put it to Google Container Registry.
  val image = docker.Image(
    "image",
    docker.ImageArgs(
      imageName = imageFullName,
      build = docker.inputs.DockerBuildArgs(
        context = p"../${appName}",
        platform = "linux/amd64" // Cloud Run only supports linux/amd64
      )
    )
  )

  // Deploy to Cloud Run. Some extra parameters like concurrency and memory are set for illustration purpose.
  val service = gcp.cloudrun.Service(
    "service",
    gcp.cloudrun.ServiceArgs(
      location = region,
      name = appName,
      template = gcp.cloudrun.inputs.ServiceTemplateArgs(
        spec = gcp.cloudrun.inputs.ServiceTemplateSpecArgs(
          containers = gcp.cloudrun.inputs.ServiceTemplateSpecContainerArgs(
            image = image.imageName,
            resources = gcp.cloudrun.inputs.ServiceTemplateSpecContainerResourcesArgs(
              limits = Map(
                "memory" -> "1Gi"
              )
            )
          ) :: Nil
        )
      )
    ),
    opts = opts(dependsOn = enableServices("run.googleapis.com"))
  )

  // Open the service to public unrestricted access
  val serviceIam = gcp.cloudrun.IamMember(
    "service-iam-everyone",
    gcp.cloudrun.IamMemberArgs(
      location = service.location,
      service = service.name,
      role = "roles/run.invoker",
      member = "allUsers"
    )
  )

  Stack(
    Output.sequence(enableServices.values),
    serviceIam
  ).exports(
    dockerImage = imageFullName,
    serviceUrl = service.statuses.map(_.headOption.map(_.url)) // Export the DNS name of the service
  )
}
