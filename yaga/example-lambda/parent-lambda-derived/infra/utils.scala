package yaga.generated.lambdatest.parent

class Lambda private(
  underlyingFunction: besom.api.aws.lambda.Function,
  lambdaHandle: yaga.extensions.aws.lambda.LambdaHandle[Lambda.Input, Lambda.Output]
) extends yaga.extensions.aws.lambda.internal.Lambda[Lambda.Input, Lambda.Output](
  underlyingFunction = underlyingFunction,
  lambdaHandle = lambdaHandle
)

object Lambda:
  type Config = yaga.generated.lambdatest.parent.Config
  type Input = yaga.generated.lambdatest.parent.Qux
  type Output = scala.Unit

  def apply(
    name: besom.util.NonEmptyString,
    args: besom.api.aws.lambda.FunctionArgs,
    config: besom.types.Input[Config],
    opts: besom.ResourceOptsVariant.Custom ?=> besom.CustomResourceOptions = besom.CustomResourceOptions()
  ): besom.types.Output[Lambda] =
    val metadata = yaga.extensions.aws.lambda.internal.LambdaHandlerUtils.lambdaHandlerMetadataFromMavenCoordinates[Config, Input, Output](
      // TODO?: Expose maven coordinates as a part of the generated API
      orgName = "org.virtuslab",
      moduleName = "parent-lambda_3", // TODO don't require scala major prefix?
      version = "0.0.1-SNAPSHOT"
    )
    val javaRuntime = "java21"

    import besom.json.DefaultJsonProtocol.given

    for
      lambda <- yaga.extensions.aws.lambda.internal.Lambda[Config, Input, Output](
        name = name,
        codeArchive = besom.types.Archive.FileArchive(metadata.artifactAbsolutePath),
        handlerClassName = metadata.handlerClassName,
        runtime = javaRuntime,
        config = config,
        args = args,
        opts = opts
      )
    yield new Lambda(
      underlyingFunction = lambda.underlyingFunction,
      lambdaHandle = lambda.lambdaHandle
    )
