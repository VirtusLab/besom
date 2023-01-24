package besom.internal

import besom.util.NonEmptyString
import scala.util.Try

type Config = Map[NonEmptyString, String] // TODO replace

case class RunInfo(
  project: NonEmptyString,
  stack: NonEmptyString,
  config: Config,
  configSecretKeys: List[NonEmptyString],
  parallel: Int,
  dryRun: Boolean,
  monitorAddress: NonEmptyString,
  engineAddress: NonEmptyString
  // mocks: MockResourceMonitor, // TODO is this necessary?
  // engineConn: Option[grpc.Connection] // TODO is this necessary?
)

object RunInfo:

  // LB: copied verbatim from pulumi-go :P

  // EnvProject is the envvar used to read the current Pulumi project name.
  private final val EnvProject = "PULUMI_PROJECT"
  // EnvStack is the envvar used to read the current Pulumi stack name.
  private final val EnvStack = "PULUMI_STACK"
  // EnvConfig is the envvar used to read the current Pulumi configuration variables.
  private final val EnvConfig = "PULUMI_CONFIG"
  // EnvConfigSecretKeys is the envvar used to read the current Pulumi configuration keys that are secrets.
  // nolint: gosec
  private final val EnvConfigSecretKeys = "PULUMI_CONFIG_SECRET_KEYS"
  // EnvParallel is the envvar used to read the current Pulumi degree of parallelism.
  private final val EnvParallel = "PULUMI_PARALLEL"
  // EnvDryRun is the envvar used to read the current Pulumi dry-run setting.
  private final val EnvDryRun = "PULUMI_DRY_RUN"
  // EnvMonitor is the envvar used to read the current Pulumi monitor RPC address.
  private final val EnvMonitor = "PULUMI_MONITOR"
  // EnvEngine is the envvar used to read the current Pulumi engine RPC address.
  private final val EnvEngine = "PULUMI_ENGINE"

  private inline def get(key: String): NonEmptyString =
    sys.env.get(key).flatMap(NonEmptyString(_)).getOrElse {
      throw new Exception(s"Error: environment variable '$key' not present!")
    }

  import upickle.default._
  implicit val fooReadWrite: ReadWriter[NonEmptyString] =
    readwriter[String].bimap[NonEmptyString](identity, NonEmptyString(_).get)

  private inline def getConfig(key: String): Try[Config] =
    Try { sys.env.get(key).map(read[Map[NonEmptyString, String]](_)).getOrElse(Map.empty) }

  private inline def getSecretConfigKeys(key: String): Try[List[NonEmptyString]] =
    Try { sys.env.get(key).map(read[List[NonEmptyString]](_)).getOrElse(List.empty) }

  def fromEnv: Result[RunInfo] = Result.evalTry(Try {
    RunInfo(
      project = get(EnvProject),
      stack = get(EnvStack),
      config = getConfig(EnvConfig).get,
      configSecretKeys = getSecretConfigKeys(EnvConfigSecretKeys).get,
      parallel = get(EnvParallel).toInt,
      dryRun = get(EnvDryRun).toBoolean,
      monitorAddress = get(EnvMonitor),
      engineAddress = get(EnvEngine)
    )
  })
