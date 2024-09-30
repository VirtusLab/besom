import besom.*
import besom.api.gcp
import gcp.projects.{Service, ServiceArgs}

sealed trait GCPService(private val name: String):
  def apply(
    disableDependentServices: Input[Boolean] = true,
    disableOnDestroy: Input[Boolean] = true
  )(using Context): Output[Service] =
    GCPService.projectService(s"$name.googleapis.com", disableDependentServices, disableOnDestroy)

object GCPService:
  case object CloudRun extends GCPService("run")
  case object Scheduler extends GCPService("cloudscheduler")
  case object SecretManager extends GCPService("secretmanager")

  private def projectService(
    name: String,
    disableDependentServices: Input[Boolean] = true,
    disableOnDestroy: Input[Boolean] = true
  )(using Context): Output[Service] =
    Service(
      s"enable-${name.replace(".", "-")}",
      ServiceArgs(
        service = name,
        // if true - at every destroy this will disable the dependent services for the whole project
        disableDependentServices = disableDependentServices,
        // if true - at every destroy this will disable the service for the whole project
        disableOnDestroy = disableOnDestroy
      )
    )
