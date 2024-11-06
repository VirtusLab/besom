package yaga.extensions.aws.lambda.internal

case class LambdaHandlerMetadata(
  handlerClassName: String,
  artifactAbsolutePath: String,
  configSchema: String,
  inputSchema: String,
  outputSchema: String
)
