package yaga.extensions.aws.lambda.internal

import besom.util.NonEmptyString
import besom.types.Archive
import besom.api.aws.lambda.{Function, FunctionArgs}
import besom.api.aws.lambda.inputs.FunctionEnvironmentArgs
import yaga.extensions.aws.lambda.LambdaHandle

class Lambda[I, O](
  val underlyingFunction: Function,
  val lambdaHandle: LambdaHandle[I, O]
):
  export underlyingFunction.*

object Lambda:
  def apply[C : EnvWriter, I, O](
    name: NonEmptyString,
    codeArchive: Archive,
    handlerClassName: String,
    runtime: String,
    config: besom.types.Input[C],
    args: FunctionArgs,
    opts: besom.ResourceOptsVariant.Custom ?=> besom.CustomResourceOptions = besom.CustomResourceOptions()
  ): besom.types.Output[Lambda[I, O]] =
    for {
      conf <- config.asOutput(isSecret = false)
      modifiedArgs = args.withArgs(
        name = name, // TODO preserve name if defined explicitly in the args?
        code = codeArchive,
        handler = handlerClassName,
        runtime = runtime,
        environment = FunctionEnvironmentArgs(
          variables = summon[EnvWriter[C]].write(conf).getOrElse(throw new Exception("Cannot serialize config to environment variables")) // TODO handle error better
        )
        // TODO clear properties conflicting with the ones from above (for env vars extending rather than overriding?)
        // TODO get and set default (minimal) java version from the jar (via the metadata) ???
      )
      fun <- Function(name, modifiedArgs, opts)
      functionName <- fun.name
      handle = LambdaHandle[I, O](
        functionName = functionName
      )
    } yield new Lambda(
      underlyingFunction = fun,
      lambdaHandle = handle
    )

  extension [I, O](output: besom.types.Output[Lambda[I, O]])
    def underlyingFunction: besom.types.Output[Function] = output.map(_.underlyingFunction)
    def lambdaHandle: besom.types.Output[LambdaHandle[I, O]] = output.map(_.lambdaHandle)

    def name: besom.types.Output[String] = output.flatMap(_.name)
    def arn: besom.types.Output[String] = output.flatMap(_.arn)
    // TODO Proxy other properties of Function
