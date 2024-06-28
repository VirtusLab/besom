package besom.codegen

import besom.codegen.Utils.{ConstValueOps, TypeReferenceOps}
import besom.codegen.metaschema.*

import scala.meta.*

case class PropertyInfo private (
  name: Name,
  isOptional: Boolean,
  baseType: Type,
  argType: Type,
  inputArgType: Type,
  defaultValue: Option[Term],
  constValue: Option[Term],
  isSecret: Boolean,
  unionMappings: List[TypeMapper.UnionMapping],
  plain: Boolean
)

object PropertyInfo:
  def from(
    propertyName: String,
    propertyDefinition: PropertyDefinition,
    isPropertyRequired: Boolean
  )(implicit logger: Logger, typeMapper: TypeMapper): PropertyInfo = {
    val isRequired =
      isPropertyRequired ||
        propertyDefinition.default.nonEmpty ||
        propertyDefinition.const.nonEmpty

    val baseType = propertyDefinition.typeReference.asScalaType()
    val argType  = propertyDefinition.typeReference.asScalaType(asArgsType = true)
    val inputArgType = propertyDefinition.typeReference match {
      case ArrayType(innerType, plainItems) =>
        scalameta.types.List(
          if (plainItems) innerType.asScalaType(asArgsType = true)
          else scalameta.types.besom.types.Input(innerType.asScalaType(asArgsType = true))
        )
      case MapType(innerType, plainProperties) =>
        scalameta.types.Map(
          scalameta.types.String,
          if (plainProperties) innerType.asScalaType(asArgsType = true)
          else scalameta.types.besom.types.Input(innerType.asScalaType(asArgsType = true))
        )
      case tp =>
        tp.asScalaType(asArgsType = true)
    }

    def enumDefaultValue(value: ConstValue) =
      typeMapper.enumValue(propertyDefinition.typeReference, value)

    def unionMap = typeMapper.unionMapping(propertyDefinition.typeReference)

    val defaultValue: Option[Term] = {
      val propertyDefaultValue =
        propertyDefinition.default
          .map(d => enumDefaultValue(d).getOrElse(d.asScala))

      val propertyConstValue =
        propertyDefinition.const
          .map(_.asScala)

      propertyDefaultValue
        .orElse(propertyConstValue)
        .orElse {
          if isPropertyRequired then None else Some(scalameta.None)
        }
    }
    val constValue = propertyDefinition.const.map(_.asScala)

    PropertyInfo(
      name = Name(manglePropertyName(propertyName)),
      isOptional = !isRequired,
      baseType = baseType,
      argType = argType,
      inputArgType = inputArgType,
      defaultValue = defaultValue,
      constValue = constValue,
      isSecret = propertyDefinition.secret,
      unionMappings = unionMap,
      plain = propertyDefinition.plain
    )
  }

  private val anyRefMethodNames = Set(
    "eq",
    "ne",
    "notify",
    "notifyAll",
    "synchronized",
    "wait",
    "asInstanceOf",
    "clone",
    "equals",
    "getClass",
    "hashCode",
    "isInstanceOf",
    "toString",
    "finalize"
  )

  private val reservedMethods = Set(
    "pulumiResourceName",
    "asString"
  )

  private val reservedPackages = Set(
    "java",
    "javax",
    "scala",
    "besom"
  )

  private val reserved = anyRefMethodNames ++ reservedMethods ++ reservedPackages

  // This logic must be undone the same way in codecs
  // Keep in sync with `unmanglePropertyName` in codecs.scala
  private def manglePropertyName(name: String)(implicit logger: Logger): String =
    if reserved.contains(name) then
      val mangledName = name + "_"
      logger.debug(s"Mangled property name '$name' as '$mangledName'")
      mangledName
    else name

end PropertyInfo
