package yaga.extensions.aws.lambda

import yaga.json.{JsonReader, JsonWriter}
import yaga.extensions.aws.lambda.internal.EnvReader

trait LambdaHandlerBase[C, I, O] extends LambdaHandlerApi:
  type Config = C
  type Input = I
  type Output = O
  given inputReader: JsonReader[Input]
  given outputWriter: JsonWriter[Output]
  given lambdaShape: LambdaShape[Config, Input, Output]

  given configReader: JsonReader[Config]
  protected def environmentVariables: Map[String, String]
  
  override protected lazy val config: Config =
    EnvReader.read[Config](environmentVariables) match
      case Right(conf) =>
        conf
      case Left(e) =>
        println("Could not read the configuration from environment variables")
        throw e
      
  override protected def context(using ctx: LambdaContext): LambdaContext = ctx

