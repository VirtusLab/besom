package besom.internal

import besom.util.NonEmptyString
import besom.json.DefaultJsonProtocol

/** Used by the codegen module in the generated code.
  */
//noinspection ScalaUnusedSymbol
object Codegen:
  def config[A: ConfigValueReader](providerName: NonEmptyString)(
    key: NonEmptyString,
    isSecret: Boolean,
    environment: List[String],
    default: Option[A]
  )(using Context): Output[Option[A]] = besom.Config(providerName).flatMap(_.getOrDefault(key, isSecret, environment, default))

/** Used by the codegen module in the generated code.
  */
//noinspection ScalaUnusedSymbol
object CodegenProtocol extends DefaultJsonProtocol