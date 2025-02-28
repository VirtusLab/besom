package yaga.extensions.aws.lambda

/**
  * C = Config
  * I = Input
  * O = Output
  */
trait LambdaHandlerApi[C, I, O]:
  protected def config: C
  protected def context(using ctx: LambdaContext): LambdaContext = ctx
  def handleInput(input: I): LambdaContext ?=> O
