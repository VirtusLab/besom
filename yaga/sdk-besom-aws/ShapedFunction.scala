package yaga.extensions.aws.lambda

import besom.util.NonEmptyString
import besom.types.{Input, Output}
import besom.api.aws.lambda.{Function, FunctionArgs}
import besom.api.aws.lambda.inputs.FunctionEnvironmentArgs

class ShapedFunction[I, O](
  val unshapedFunction: Function,
  val functionHandle: ShapedFunctionHandle[I, O]
):
  export unshapedFunction.*

object ShapedFunction:
  export ShapedLambdaUtils.lambdaHandlerMetadataFromLocalJar

  def apply[C : EnvWriter, I, O](
    name: NonEmptyString,
    metadata: Input[LambdaHandlerMetadata[C, I, O]],
    config: Input[C],
    args: FunctionArgs,
    opts: besom.ResourceOptsVariant.Custom ?=> besom.CustomResourceOptions = besom.CustomResourceOptions()
  ): Output[ShapedFunction[I, O]] =
    for {
      meta <- metadata.asOutput(isSecret = false)
      conf <- config.asOutput(isSecret = false)
      modifiedArgs = args.withArgs(
        code = meta.codeArchive,
        handler = meta.handlerClassName,
        environment = FunctionEnvironmentArgs(
          variables = summon[EnvWriter[C]].write(conf).getOrElse(throw new Exception("Cannot serialize config to environment variables")) // TODO handle error better
        )
        // TODO clear properties conflicting with the ones from above (for env vars extending rather than overriding?)
      )
      fun <- Function(name, modifiedArgs, opts)
      functionName <- fun.name
      handle = ShapedFunctionHandle[I, O](
        functionName = functionName
      )
    } yield new ShapedFunction(
      unshapedFunction = fun,
      functionHandle = handle
    )

  extension [I, O](output: Output[ShapedFunction[I, O]])
    def unshapedFunction: Output[Function] = output.map(_.unshapedFunction)
    def functionHandle: Output[ShapedFunctionHandle[I, O]] = output.map(_.functionHandle)

    def name: Output[String] = output.flatMap(_.name)
    def arn: Output[String] = output.flatMap(_.arn)
    // TODO Proxy other properties of Function
