package yaga.extensions.aws.lambda

import scala.concurrent.Future

trait LambdaHandlerApi:
  type Config
  type Input
  type Output
  protected def config: Config
  protected def context(using ctx: LambdaContext): LambdaContext = ctx

trait LambdaHandlerSyncApi extends LambdaHandlerApi:
  def handleInput(input: Input): LambdaContext ?=> Output

trait LambdaHandlerAsyncApi extends LambdaHandlerApi:
  def handleInput(input: Input): LambdaContext ?=> Future[Output]
