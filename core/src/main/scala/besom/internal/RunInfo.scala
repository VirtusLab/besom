package besom.internal

import besom.internal.logging.{LocalBesomLogger => logger}
import besom.util.NonEmptyString
import scala.util.Try

case class RunInfo(
  project: NonEmptyString,
  stack: NonEmptyString,
  // config: Config,
  // configSecretKeys: Set[NonEmptyString],
  parallel: Int,
  dryRun: Boolean,
  monitorAddress: NonEmptyString,
  engineAddress: NonEmptyString
  // mocks: MockResourceMonitor, // TODO is this necessary?
  // engineConn: Option[grpc.Connection] // TODO is this necessary?
)

object RunInfo:
  import Env.*

  def fromEnv: Result[RunInfo] =
    Result
      .evalTry(Try {
        RunInfo(
          project = Env.getOrFail(EnvProject),
          stack = Env.getOrFail(EnvStack),
          parallel = Env.getOrFail(EnvParallel).toInt,
          dryRun = Env.getOrFail(EnvDryRun).toBoolean,
          monitorAddress = Env.getOrFail(EnvMonitor),
          engineAddress = Env.getOrFail(EnvEngine)
        )
      })
      .transformM {
        case Left(error) =>
          logger.error(s"Error during initial run configuration resolution: ${error.getMessage}") *>
            Result.pure(Left(error))
        case Right(runInfo) =>
          logger.debug(s"Run configuration resolved successfully!") *> logger.trace(s"${pprint(runInfo)}") *>
            Result.pure(Right(runInfo))
      }
