package besom.codegen

import besom.codegen.PulumiTypeReference.TypeReferenceOps
import besom.codegen.Utils.ConstValueOps

import scala.meta._
import scala.meta.dialects.Scala33
import besom.codegen.metaschema._

case class PropertyInfo(
  name: Term.Name,
  isOptional: Boolean,
  baseType: Type,
  argType: Type,
  inputArgType: Type,
  defaultValue: Option[Term],
  constValue: Option[Term],
  isSecret: Boolean
) {
  def asScalaParam: Term.Param = {
    val paramType = if (this.isOptional) scalameta.types.Option(this.baseType) else this.baseType
    Term
      .Param(
        mods = List.empty,
        name = this.name,
        decltpe = Some(paramType),
        default = None
      )
  }

  def asOutputParam: Term.Param = {
    val paramType = if (this.isOptional) scalameta.types.Option(this.baseType) else this.baseType
    Term
      .Param(
        mods = List.empty,
        name = this.name,
        decltpe = Some(scalameta.types.besom.types.Output(paramType)),
        default = None
      )
  }

  def asScalaGetter: Stat = {
    val innerType = if (this.isOptional) scalameta.types.Option(this.baseType) else this.baseType
    val code = s"""def ${this.name.syntax}: ${scalameta.types.besom.types.Output(innerType)} = output.map(_.${this.name.syntax})"""
    scalameta.parse(code)
  }

  def asScalaOptionGetter: Stat = {
    val mapOrFlatmap = if (this.isOptional) Term.Name("flatMap") else Term.Name("map")
    val code = s"""def ${this.name.syntax}: ${scalameta.types.besom.types.Output(scalameta.types.Option(this.baseType))} = output.map(_.${mapOrFlatmap.syntax}(_.${this.name.syntax}))"""
    scalameta.parse(code)
  }

  val asScalaNamedArg: Term.Assign = {
    val isSecret = Lit.Boolean(this.isSecret)
    val argValue = this.constValue match {
      case Some(constValue) => scalameta.besom.types.Output(constValue)
      case None =>
        if (this.isOptional) Term.Apply(scalameta.method(this.name, "asOptionOutput"), Term.ArgClause(Term.Assign(Term.Name("isSecret"), isSecret) :: Nil))
        else Term.Apply(scalameta.method(this.name, "asOutput"), Term.ArgClause(Term.Assign(Term.Name("isSecret"), isSecret) :: Nil))
    }
    Term.Assign(this.name, argValue)
  }
}

object PropertyInfo {
  def from(
    propertyName: String,
    propertyDefinition: PropertyDefinition,
    isPropertyRequired: Boolean
  )(implicit thisPackageInfo: ThisPackageInfo, schemaProvider: SchemaProvider, logger: Logger): Either[Exception, PropertyInfo] = {
    val isRequired =
      isPropertyRequired ||
        propertyDefinition.default.nonEmpty ||
        propertyDefinition.const.nonEmpty

    val baseType = propertyDefinition.typeReference.asScalaType()
    val argType  = propertyDefinition.typeReference.asScalaType(asArgsType = true)
    val inputArgType = propertyDefinition.typeReference match {
      case ArrayType(innerType) =>
        for {
          t <- innerType.asScalaType(asArgsType = true)
        } yield scalameta.types.List(scalameta.types.besom.types.Input(t))
      case MapType(innerType) =>
        for {
          t <- innerType.asScalaType(asArgsType = true)
        } yield scalameta.types.Map(scalameta.types.String, scalameta.types.besom.types.Input(t))
      case tp =>
        tp.asScalaType(asArgsType = true)
    }
    val defaultValue =
      propertyDefinition.default
        .orElse(propertyDefinition.const)
        .map { value => value.asScala }
        .orElse {
          if (isPropertyRequired) None else Some(scalameta.None)
        }
    val constValue = propertyDefinition.const.map(_.asScala)

    for {
      baseType     <- baseType
      argType      <- argType
      inputArgType <- inputArgType
    } yield PropertyInfo(
      name = Term.Name(manglePropertyName(propertyName)),
      isOptional = !isRequired,
      baseType = baseType,
      argType = argType,
      inputArgType = inputArgType,
      defaultValue = defaultValue,
      constValue = constValue,
      isSecret = propertyDefinition.secret
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
    "toString"
  )

  // This logic must be undone the same way in codecs
  // Keep in sync with `unmanglePropertyName` in codecs.scala
  private def manglePropertyName(name: String)(implicit logger: Logger): String =
    if (anyRefMethodNames.contains(name)) {
      val mangledName = name + "_"
      logger.warn(s"Mangled property name '$name' as '$mangledName'")
      mangledName
    } else name
}
