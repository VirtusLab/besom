package besom.internal

import besom.internal.logging.{LocalBesomLogger => logger}
import besom.util.NonEmptyString
import scala.util.Try

case class RunInfo(
  project: NonEmptyString,
  stack: NonEmptyString,
  acceptResources: Boolean,
  parallel: Int,
  dryRun: Boolean,
  monitorAddress: NonEmptyString,
  engineAddress: NonEmptyString
)

object RunInfo:
  import Env.*

  def fromEnv: Result[RunInfo] =
    Result
      .evalTry(Try {
        RunInfo(
          project = Env.project,
          stack = Env.stack,
          acceptResources = Env.acceptResources,
          parallel = Env.parallel, // TODO we don't use this, should we?
          dryRun = Env.dryRun,
          monitorAddress = Env.monitorAddress,
          engineAddress = Env.engineAddress
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
