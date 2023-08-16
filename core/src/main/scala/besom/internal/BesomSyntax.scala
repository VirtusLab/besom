package besom.internal

import besom.internal.logging.BesomLogger
import scala.reflect.Typeable
import besom.util.NonEmptyString
import besom.types.ResourceType
import besom.types.URN

/*
 * This trait is the main export point that exposes Besom specific functions and types to the user.
 * The only exception is the Output object which is exposed in BesomModule which extends this trait.
 */
trait BesomSyntax:
  def isDryRun(using ctx: Context): Boolean = ctx.isDryRun

  def config(using ctx: Context): Config = ctx.config

  def log(using ctx: Context): besom.aliases.Logger =
    besom.internal.logging.UserLoggerFactory(using ctx)

  def urn(using ctx: Context): Output[URN] =
    Output.ofData(ctx.getParentURN.map(OutputData(_)))

  val exports: Export.type = Export

  def component[A <: ComponentResource & Product: RegistersOutputs: Typeable](name: NonEmptyString, typ: ResourceType)(
    f: Context ?=> ComponentBase ?=> A | Output[A]
  )(using ctx: Context): Output[A] =
    Output.ofData {
      ctx
        .registerComponentResource(name, typ)
        .flatMap { componentBase =>
          val urnRes: Result[URN] = componentBase.urn.getValueOrFail {
            s"Urn for component resource $name is not available. This should not happen."
          }

          val componentContext = ComponentContext(ctx, urnRes)
          val componentOutput =
            try
              f(using componentContext)(using componentBase) match
                case output: Output[A] @unchecked => output
                case a: A                         => Output(Result.pure(a))
            catch case e: Exception => Output(Result.fail(e))

          val componentResult = componentOutput.getValueOrFail {
            s"Component resource $name of type $typ did not return a value. This should not happen."
          }

          componentResult.flatMap { a =>
            val serializedOutputs = RegistersOutputs[A].serializeOutputs(a)
            ctx.registerResourceOutputs(name, typ, urnRes, serializedOutputs) *> Result.pure(a)
          }
        }
        .map(OutputData(_))
    }
