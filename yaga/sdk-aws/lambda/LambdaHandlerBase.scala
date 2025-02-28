package yaga.extensions.aws.lambda

import yaga.json.JsonReader
import yaga.extensions.aws.lambda.internal.EnvReader

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker


trait LambdaHandlerBase[C, I, O](using configReader: JsonReader[C]) extends LambdaHandlerApi[C, I, O]:
  protected def environmentVariables: Map[String, String]
  
  override protected lazy val config: C =
    EnvReader.read(environmentVariables) match
      case Right(conf) =>
        conf
      case Left(e) =>
        println("Could not read the configuration from environment variables")
        throw e
      
  override protected def context(using ctx: LambdaContext): LambdaContext = ctx

