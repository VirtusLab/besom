package yaga.extensions.aws.lambda

import scala.concurrent.Future

trait LambdaHandlerApi:
  type Config
  type Input
  type Output
  protected def config: Config
  protected def context(using ctx: LambdaContext): LambdaContext = ctx
  protected def handleInputAsync(input: Input): LambdaContext ?=> Future[Output]


trait LambdaHandlerSyncApi extends LambdaHandlerApi:
  import scala.concurrent.ExecutionContext.Implicits.global

  def handleInput(input: Input): LambdaContext ?=> Output
  protected def handleInputAsync(input: Input): LambdaContext ?=> Future[Output] = Future { handleInput(input) }

trait LambdaHandlerAsyncApi extends LambdaHandlerApi:
  self: LambdaHandlerApi => 
  def handleInput(input: Input): LambdaContext ?=> Future[Output]
  protected def handleInputAsync(input: Input): LambdaContext ?=> Future[Output] = handleInput(input)
