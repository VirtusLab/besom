package yaga.extensions.aws.lambda

import scala.quoted.*
import yaga.extensions.aws.model.Schema
import yaga.extensions.aws.lambda.LambdaHandler

trait LambdaShape[T]:
  def configSchema: String
  def inputSchema: String
  def outputSchema: String

object LambdaShape:
  inline def derived[T] = ${ derivedImpl[T] }

  def derivedImpl[T : Type](using Quotes): Expr[LambdaShape[T]] =
    import quotes.reflect.*

    val typeSymbol = TypeRepr.of[T].typeSymbol
    val isObject = typeSymbol.flags.is(Flags.Module)
    val isTrait = typeSymbol.flags.is(Flags.Trait)
    val isAbstract = typeSymbol.flags.is(Flags.Abstract)
    val hasParameterlessConstructor = typeSymbol.declarations.filter(_.isClassConstructor).exists(sym => sym.signature.paramSigs.isEmpty)
    val isLambdaHandler = TypeRepr.of[T] <:< TypeRepr.of[LambdaHandler[?, ?, ?]]
    val typeFullName = Type.show[T]
    val lambdaHandlerTypeFullName = Type.show[LambdaHandler]

    if isObject then
      throw new Exception(s"Type ${typeFullName} should be a class rather than an object to work as a lambda handler")
    if isTrait then
      throw new Exception(s"Type ${typeFullName} should be a class rather than a trait to work as a lambda handler")
    if isAbstract then
      throw new Exception(s"Type ${typeFullName} should be a non-abstract class rather than an object to work as a lambda handler")
    if !hasParameterlessConstructor then
      throw new Exception(s"Type ${typeFullName} needs to have a parameterless constructor to work as a lambda handler")
    if !isLambdaHandler then
      throw new Exception(s"Type ${typeFullName} should extend ${lambdaHandlerTypeFullName} to work as a lambda handler")

    val (confSchema, inSchema, outSchema) = Type.of[T] match
      case '[LambdaHandler[c, i, o]] =>
        (Expr(Schema.getSchemaStr[c]), Expr(Schema.getSchemaStr[i]), Expr(Schema.getSchemaStr[o]))

    '{
      new LambdaShape[T] {
        override def configSchema: String = ${ confSchema }
        override def inputSchema: String = ${ inSchema }
        override def outputSchema: String = ${ outSchema }
      }
    }
