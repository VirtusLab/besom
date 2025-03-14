package yaga.extensions.aws.lambda

private [lambda] trait LambdaClientCompanion:
  type UnderlyingLambdaClient

  def apply(
    underlyingClient: UnderlyingLambdaClient = defaultUnderlyingClient
  ): LambdaClientApi

  protected def defaultUnderlyingClient: UnderlyingLambdaClient
