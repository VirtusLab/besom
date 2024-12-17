package yaga.extensions.aws.lambda

import scala.language.implicitConversions
import yaga.extensions.aws.model.{SchemaProvider, TypeSchemasCompatibility}
import besom.json.*

class LambdaHandle[I, O](
  val functionName: String // TODO use ARN instead?
)

object LambdaHandle:
  private case class LambdaHandleStruct(
    functionName: String,
    inputSchema: String,
    outputSchema: String
  ) derives JsonFormat

  given[I : SchemaProvider, O : SchemaProvider]: JsonFormat[LambdaHandle[I, O]] with
    def write(obj: LambdaHandle[I, O]): JsValue =
      val struct = LambdaHandleStruct(
        functionName = obj.functionName,
        inputSchema = summon[SchemaProvider[I]].schemaStr,
        outputSchema = summon[SchemaProvider[O]].schemaStr
      )
      summon[JsonWriter[LambdaHandleStruct]].write(struct)

    def read(json: JsValue): LambdaHandle[I, O] =
      val expectedInputSchema = summon[SchemaProvider[I]].schemaStr
      val expectedOutputSchema = summon[SchemaProvider[O]].schemaStr
      val struct = summon[JsonReader[LambdaHandleStruct]].read(json)
      assert(struct.inputSchema == expectedInputSchema, s"LambdaHandle input schema mismatch - expected: $expectedInputSchema, but got ${struct.inputSchema}")
      assert(struct.outputSchema == expectedOutputSchema, s"LambdaHandle output schema mismatch - expected: $expectedOutputSchema, but got ${struct.outputSchema}")
      LambdaHandle[I, O](
        functionName = struct.functionName
      )

  implicit def compatibleSchemaConvertion[I1, O1, I2, O2](
    handle: LambdaHandle[I1, O1]
  )(using TypeSchemasCompatibility[I1, I2], TypeSchemasCompatibility[O1, O2]): LambdaHandle[I2, O2] =
    LambdaHandle[I2, O2](functionName = handle.functionName)