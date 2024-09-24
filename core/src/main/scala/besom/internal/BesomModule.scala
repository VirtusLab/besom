package besom.internal

import besom.internal.logging.{BesomMDC, LocalBesomLogger => logger, BesomLogger}
import besom.util.printer

/** An abstract effect Besom module, which can be implemented for different effect types.
  * @tparam Eff
  *   the effect type
  * @see
  *   [[besom.Pulumi]]
  * @see
  *   [[besom.internal.BesomModule]]
  * @see
  *   [[besom.internal.BesomSyntax]]
  */
trait EffectBesomModule extends BesomSyntax:
  type Eff[+A]

  protected lazy val rt: Runtime[Eff]

  /** Run a [[besom.Pulumi]] program with a Besom [[besom.Context]] and export Stack outputs.
    *
    * Inside `Pulumi.run` block you can use the following methods without `Pulumi.` prefix: 
    *  - [[besom.internal.BesomSyntax.isDryRun]] - is the current run a dry run (planning instead of deployment)
    *  - [[besom.internal.BesomSyntax.config]] - provides access to the configuration and secrets
    *  - [[besom.internal.BesomSyntax.log]] - all your logging needs
    *  - [[besom.internal.BesomSyntax.urn]] - the URN of the current Pulumi stack
    *  - [[besom.internal.BesomSyntax.pulumiOrganization]] - the organization of the current Pulumi stack
    *  - [[besom.internal.BesomSyntax.pulumiProject]] - the project name of the current Pulumi stack
    *  - [[besom.internal.BesomSyntax.pulumiStack]] - the stack name of the current Pulumi stack
    * 
    *  Should you need to use 
    * one of these methods outside of the `Pulumi.run` block, you can use the following using clauses:
    *  - `(using Context)` if you have a `import besom.*` clause on top of the file or
    *  - `(using besom.Context)` using a fully qualified name of the type.
    * 
    * There are also two other functions exported by [[besom.Pulumi]]:
    *  - [[besom.internal.BesomModule.component]] - creates a new component resource
    *  - [[besom.internal.BesomModule.opts]] - shortcut function allowing for uniform resource options syntax 
    *
    * Example:
    * {{{
    * import besom.*
    * import besom.api.aws
    *
    * @main def run = Pulumi.run {
    *   val bucket = aws.s3.Bucket("my-bucket")
    *
    *   Stack.exports(bucketUrl = bucket.websiteEndpoint)
    * }
    * }}}
    *
    * @param program
    *   the program to run
    */
  def run(program: Context ?=> Stack): Unit =
    given emptyMDC: BesomMDC[_] = BesomMDC.empty

    val everything: Result[Unit] = Result.scoped {
      for
        _              <- BesomLogger.setupLogger()
        runInfo        <- RunInfo.fromEnv
        _              <- logger.debug(s"Besom starting up in ${if runInfo.dryRun then "dry run" else "live"} mode.")
        taskTracker    <- TaskTracker()
        monitor        <- Result.resource(Monitor(runInfo.monitorAddress))(_.close())
        engine         <- Result.resource(Engine(runInfo.engineAddress))(_.close())
        _              <- logger.debug(s"Established connections to monitor and engine, spawning streaming pulumi logger.")
        logger         <- Result.resource(BesomLogger(engine, taskTracker))(_.close())
        config         <- Config.forProject(runInfo.project)
        featureSupport <- FeatureSupport(monitor)
        _              <- logger.trace(s"Environment:\n${sys.env.toSeq.sortBy(_._1).map((k, v) => s"$k: $v").mkString("\n")}")
        _              <- logger.debug(s"Resolved feature support: ${printer.render(featureSupport)}")
        _              <- logger.debug(s"Spawning context and executing user program.")
        ctx            <- Context(runInfo, taskTracker, monitor, engine, logger, featureSupport, config)
        _              <- logger.debug(s"Context established, executing user program.")
        stack          <- Result.defer(program(using ctx)) // for formatting ffs
        _              <- logger.debug(s"User program executed, evaluating returned Stack.")
        _              <- stack.evaluateDependencies(using ctx)
        _              <- StackResource.registerStackOutputs(runInfo, stack.getExports.result)(using ctx)
        _              <- ctx.waitForAllTasks
      yield ()
    }

    rt.unsafeRunSync(everything.run(using rt)) match
      case Left(err) =>
        scribe.error(err)
        throw err
      case Right(_) =>
        sys.exit(0)
  end run
end EffectBesomModule

/** The Besom module provides the core functionality of the Besom runtime.
  * @see
  *   [[besom.Pulumi]]
  * @see
  *   [[besom.internal.EffectBesomModule]]
  */
trait BesomModule extends EffectBesomModule {
  export besom.types.{*, given}
  export besom.util.interpolator.{*, given}
}
