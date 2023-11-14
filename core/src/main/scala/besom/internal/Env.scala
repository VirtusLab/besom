package besom.internal

import besom.util.NonEmptyString
import scala.util.Try

object Env:
  // LB: copied verbatim from pulumi-go :P

  // EnvProject is the envvar used to read the current Pulumi project name.
  private[internal] final val EnvProject = "PULUMI_PROJECT"
  // EnvStack is the envvar used to read the current Pulumi stack name.
  private[internal] final val EnvStack = "PULUMI_STACK"
  // EnvOrganization is the envvar used to read the current Pulumi organization name.
  private[internal] final val EnvOrganization = "PULUMI_ORGANIZATION"
  // EnvConfig is the envvar used to read the current Pulumi configuration variables.
  private[internal] final val EnvConfig = "PULUMI_CONFIG"
  // EnvConfigSecretKeys is the envvar used to read the current Pulumi configuration keys that are secrets.
  // nolint: gosec
  private[internal] final val EnvConfigSecretKeys = "PULUMI_CONFIG_SECRET_KEYS"
  // EnvParallel is the envvar used to read the current Pulumi degree of parallelism.
  private[internal] final val EnvParallel = "PULUMI_PARALLEL"
  // EnvDryRun is the envvar used to read the current Pulumi dry-run setting.
  private[internal] final val EnvDryRun = "PULUMI_DRY_RUN"
  // EnvMonitor is the envvar used to read the current Pulumi monitor RPC address.
  private[internal] final val EnvMonitor = "PULUMI_MONITOR"
  // EnvEngine is the envvar used to read the current Pulumi engine RPC address.
  private[internal] final val EnvEngine = "PULUMI_ENGINE"

  // TODO
  private[internal] final val EnvDisableResourceReferences = "PULUMI_DISABLE_RESOURCE_REFERENCES"

  private[internal] final val EnvEnableTraceLoggingToFile = "PULUMI_ENABLE_TRACE_LOGGING_TO_FILE"

  private[internal] final val EnvLogLevel = "PULUMI_BESOM_LOG_LEVEL"

  private[internal] def getOrFail(key: String): NonEmptyString =
    sys.env.get(key).flatMap(NonEmptyString(_)).getOrElse {
      throw new Exception(s"Error: environment variable '$key' not present!")
    }

  private[internal] def getMaybe(key: String): Option[NonEmptyString] =
    sys.env.get(key).flatMap(NonEmptyString(_))

  import spray.json._, DefaultJsonProtocol._

  given nesJF(using jfs: JsonFormat[String]): JsonFormat[NonEmptyString] =
    new JsonFormat[NonEmptyString]:
      def read(json: JsValue): NonEmptyString = NonEmptyString(jfs.read(json)).get
      def write(nes: NonEmptyString): JsValue = jfs.write(nes)

  private[internal] def getConfigMap(key: String): Try[Map[NonEmptyString, String]] =
    Try { sys.env.get(key).map(_.parseJson.convertTo[Map[NonEmptyString, String]]).getOrElse(Map.empty) }

  private[internal] def getConfigSecretKeys(key: String): Try[Set[NonEmptyString]] =
    Try { sys.env.get(key).map(_.parseJson.convertTo[Set[NonEmptyString]]).getOrElse(Set.empty) }

  private[internal] def isTruthy(s: String): Boolean =
    s == "1" || s.equalsIgnoreCase("true")

  private[internal] def isNotTruthy(s: String): Boolean = !isTruthy(s)

  lazy val logLevel        = getMaybe(EnvLogLevel).flatMap(scribe.Level.get(_)).getOrElse(scribe.Level.Warn)
  lazy val traceRunToFile  = getMaybe(EnvEnableTraceLoggingToFile).map(isTruthy).getOrElse(false)
  lazy val project         = getOrFail(EnvProject)
  lazy val stack           = getOrFail(EnvStack)
  lazy val organization    = getMaybe(EnvOrganization)
  lazy val acceptResources = getMaybe(EnvDisableResourceReferences).map(isNotTruthy(_)).getOrElse(true)
  lazy val parallel        = getOrFail(EnvParallel).toInt
  lazy val dryRun          = getOrFail(EnvDryRun).toBoolean
  lazy val monitorAddress  = getOrFail(EnvMonitor)
  lazy val engineAddress   = getOrFail(EnvEngine)
