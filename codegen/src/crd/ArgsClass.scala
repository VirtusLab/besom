package besom.codegen.crd

import besom.codegen.scalameta.interpolator.*
import besom.codegen.*
import scala.meta.*
import scala.meta.dialects.Scala33
import besom.codegen.scalameta.types

object ArgsClass:
  def makeArgsClassSourceFile(
    argsClassName: meta.Type.Name,
    packagePath: PackagePath,
    properties: Seq[FieldTypeInfo],
    additionalCodecs: List[AdditionalCodecs]
  ): SourceFile = {
    val argsClass = makeArgsClass(
      argsClassName = argsClassName,
      properties = properties
    )
    val argsCompanion = makeArgsCompanion(
      argsClassName = argsClassName,
      properties = properties,
      additionalCodecs = additionalCodecs
    )

    val fileContent =
      m"""|package ${packagePath.path.mkString(".")}
          |
          |$argsClass
          |
          |$argsCompanion
          |""".stripMargin.parse[Source].get
    SourceFile(
      filePath = FilePath(packagePath.path :+ s"$argsClassName.scala"),
      sourceCode = fileContent.syntax
    )
  }

  private def makeArgsClass(argsClassName: meta.Type, properties: Seq[FieldTypeInfo]): Stat = {
    val argsClassParams = properties.flatMap { propertyInfo =>
      val paramType =
        if propertyInfo.isOptional
        then types.besom.types.Output(types.Option(propertyInfo.baseType))
        else types.besom.types.Output(propertyInfo.baseType)

      val termParam = Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(paramType),
        default = None
      )

      val docs = propertyInfo.description.map(_.mkString(s"  /**\n   * ", s"\n   * ", s"\n   */"))
      Seq(docs, Some(s"  ${termParam.syntax},")).flatten
    }

    m"""|final case class $argsClassName private(
        |${argsClassParams.mkString("\n")}
        |) derives besom.Decoder, besom.Encoder""".stripMargin.parse[Stat].get
  }

  private def makeArgsCompanion(
    argsClassName: meta.Type.Name,
    properties: Seq[FieldTypeInfo],
    additionalCodecs: List[AdditionalCodecs]
  ): Stat = {
    val argsCompanionApplyParams = properties
      .map { propertyInfo =>
        val paramType =
          if propertyInfo.isOptional
          then types.besom.types.InputOptional(propertyInfo.baseType)
          else types.besom.types.Input(propertyInfo.baseType)

        val defaultValue =
          if propertyInfo.isOptional then Some(scalameta.None) else None

        Term.Param(
          mods = List.empty,
          name = propertyInfo.name,
          decltpe = Some(paramType),
          default = defaultValue
        )
      }

    val argsCompanionApplyBodyArgs = properties.map { propertyInfo =>
      val isSecret = Lit.Boolean(propertyInfo.isSecret)
      val argValue =
        if (propertyInfo.isOptional)
          m"${propertyInfo.name}.asOptionOutput(isSecret = $isSecret)".parse[Term].get
        else
          m"${propertyInfo.name}.asOutput(isSecret = $isSecret)".parse[Term].get

      Term.Assign(Term.Name(propertyInfo.name.value), argValue)
    }

    val outputExtensionMethods = properties.map { propertyInfo =>
      val innerType =
        if propertyInfo.isOptional
        then scalameta.types.Option(propertyInfo.baseType)
        else propertyInfo.baseType

      m"""def ${propertyInfo.name}: besom.types.Output[$innerType] = output.flatMap(_.${propertyInfo.name})"""
        .parse[Stat]
        .get
    }
    val optionOutputExtensionMethods = properties.map { propertyInfo =>
      val innerMethodName =
        if propertyInfo.isOptional then m"flatMapOpt" else m"mapOpt"

      m"""def ${propertyInfo.name}: besom.types.Output[scala.Option[${propertyInfo.baseType}]] = output.$innerMethodName(_.${propertyInfo.name})"""
        .parse[Stat]
        .get
    }

    val argsCompanionWithArgsParams = argsCompanionApplyParams.map { param =>
      param.copy(default = Some(m"cls.${param.name.syntax}".parse[Term].get))
    }

    m"""|object $argsClassName:
        |  def apply(
        |${argsCompanionApplyParams.map(arg => s"    ${arg.syntax}").mkString(",\n")}
        |  )(using besom.types.Context): $argsClassName =
        |    new $argsClassName(
        |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg.syntax}").mkString(",\n")}
        |    )
        |
        |  extension (cls: $argsClassName) def withArgs(
        |${argsCompanionWithArgsParams.map(arg => s"    ${arg.syntax}").mkString(",\n")}
        |  )(using besom.types.Context): $argsClassName =
        |    new $argsClassName(
        |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg.syntax}").mkString(",\n")}
        |    )
        |
        |  given outputOps: {} with
        |    extension(output: besom.types.Output[$argsClassName])
        |${outputExtensionMethods.map(meth => m"      ${meth.syntax}").mkString("\n")}
        |
        |  given optionOutputOps: {} with
        |    extension(output: besom.types.Output[scala.Option[$argsClassName]])
        |${optionOutputExtensionMethods.map(meth => m"      ${meth.syntax}").mkString("\n")}
        |
        |${additionalCodecs.flatMap(_.codecs).mkString("\n")}
        |$outputOptionExtension
        |""".stripMargin.parse[Stat].get
  }

  private val outputOptionExtension =
    m"""|  extension [A](output: besom.types.Output[scala.Option[A]])
        |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
        |      output.flatMap(
        |        _.map(f)
        |          .getOrElse(output.map(_ => scala.None))
        |      )
        |
        |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
        |      flatMapOpt(f(_).map(Some(_)))""".stripMargin.parse[Stat].get
end ArgsClass
