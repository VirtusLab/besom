package yaga.extensions.aws.lambda

import yaga.shapes.SchemaProvider
import besom.json.*

class ShapedFunctionHandle[I, O](
  val functionName: String // TODO use ARN instead?
)

object ShapedFunctionHandle:
  private case class ShapedFunctionHandleStruct(
    functionName: String,
    inputSchema: String,
    outputSchema: String
  ) derives JsonFormat

  given[I : SchemaProvider, O : SchemaProvider]: JsonFormat[ShapedFunctionHandle[I, O]] with
    def write(obj: ShapedFunctionHandle[I, O]): JsValue =
      val struct = ShapedFunctionHandleStruct(
        functionName = obj.functionName,
        inputSchema = summon[SchemaProvider[I]].schemaStr,
        outputSchema = summon[SchemaProvider[O]].schemaStr
      )
      summon[JsonWriter[ShapedFunctionHandleStruct]].write(struct)

    def read(json: JsValue): ShapedFunctionHandle[I, O] =
      val expectedInputSchema = summon[SchemaProvider[I]].schemaStr
      val expectedOutputSchema = summon[SchemaProvider[O]].schemaStr
      val struct = summon[JsonReader[ShapedFunctionHandleStruct]].read(json)
      assert(struct.inputSchema == expectedInputSchema, s"ShapedFunctionHandle input schema mismatch - expected: $expectedInputSchema, but got ${struct.inputSchema}")
      assert(struct.outputSchema == expectedOutputSchema, s"ShapedFunctionHandle output schema mismatch - expected: $expectedOutputSchema, but got ${struct.outputSchema}")
      ShapedFunctionHandle[I, O](
        functionName = struct.functionName
      )