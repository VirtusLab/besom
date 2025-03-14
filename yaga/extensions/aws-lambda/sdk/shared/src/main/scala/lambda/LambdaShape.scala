package yaga.extensions.aws.lambda

import scala.quoted.*
import yaga.extensions.aws.model.Schema
import yaga.extensions.aws.lambda.LambdaHandler

trait LambdaShape[C, I, O]

object LambdaShape:
  inline given instance[C, I, O]: LambdaShape[C, I, O] = ${ instanceImpl[C, I, O] }

  def instanceImpl[C : Type, I : Type, O : Type](using Quotes): Expr[LambdaShape[C, I, O]] =
    import quotes.reflect.*

    val configSchema = Expr(Schema.getSchemaStr[C])
    val inputSchema = Expr(Schema.getSchemaStr[I])
    val outputSchema = Expr(Schema.getSchemaStr[O])

    // TODO verify the lambda handler fulfills the requirements for JDK AWS lambda handler classes

    // val typeSymbol = TypeRepr.of[T].typeSymbol
    // val isObject = typeSymbol.flags.is(Flags.Module)
    // val isTrait = typeSymbol.flags.is(Flags.Trait)
    // val isAbstract = typeSymbol.flags.is(Flags.Abstract)
    // val hasParameterlessConstructor = typeSymbol.declarations.filter(_.isClassConstructor).exists(sym => sym.signature.paramSigs.isEmpty)
    // val isLambdaHandler = TypeRepr.of[T] <:< TypeRepr.of[LambdaHandler[?, ?, ?]]
    // val typeFullName = Type.show[T]
    // val lambdaHandlerTypeFullName = Type.show[LambdaHandler]

    // if isObject then
    //   throw new Exception(s"Type ${typeFullName} should be a class rather than an object to work as a lambda handler")
    // if isTrait then
    //   throw new Exception(s"Type ${typeFullName} should be a class rather than a trait to work as a lambda handler")
    // if isAbstract then
    //   throw new Exception(s"Type ${typeFullName} should be a non-abstract class rather than an object to work as a lambda handler")
    // if !hasParameterlessConstructor then
    //   throw new Exception(s"Type ${typeFullName} needs to have a parameterless constructor to work as a lambda handler")
    // if !isLambdaHandler then
    //   throw new Exception(s"Type ${typeFullName} should extend ${lambdaHandlerTypeFullName} to work as a lambda handler")


    '{
      new LambdaShape[C, I, O] {
        @yaga.extensions.aws.model.annotations.ConfigSchema(${configSchema})
        @yaga.extensions.aws.model.annotations.InputSchema(${inputSchema})
        @yaga.extensions.aws.model.annotations.OutputSchema(${outputSchema})
        class SchemaPlaceholder
      }
    }
