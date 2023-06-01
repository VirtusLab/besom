package besom.internal

import scala.util.Try

case class RunOptions(acceptResources: Boolean, logLevel: scribe.Level, traceRunToFile: Boolean)

object RunOptions:
  import Env.*

  def fromEnv: Result[RunOptions] = Result.defer {
    RunOptions(
      acceptResources = Env.getMaybe(EnvDisableResourceReferences).map(isNotTruthy(_)).getOrElse(true),
      logLevel = Env.getMaybe(EnvLogLevel).flatMap(scribe.Level.get(_)).getOrElse(scribe.Level.Warn),
      traceRunToFile =
        Env.getMaybe(EnvEnableTraceLoggingToFile).flatMap(s => Try(s.toBoolean).toOption).getOrElse(false)
    )
  }
