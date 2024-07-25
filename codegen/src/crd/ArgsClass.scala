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
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin.parse[Source].get
    SourceFile(
      filePath = FilePath(packagePath.path :+ s"$argsClassName.scala"),
      sourceCode = fileContent.syntax
    )
  }

  private def makeArgsClass(argsClassName: meta.Type, properties: Seq[FieldTypeInfo]): Stat = {
    val argsClassParams = properties.map { propertyInfo =>
      val paramType =
        if propertyInfo.isOptional
        then types.besom.types.Output(types.Option(propertyInfo.baseType))
        else types.besom.types.Output(propertyInfo.baseType)

      Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(paramType),
        default = None
      )
    }

    m"""|final case class $argsClassName private(
        |${argsClassParams.map(arg => m"  ${arg.syntax}").mkString(",\n")}
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
          m"${propertyInfo.name}.asOptionOutput(isSecret = ${isSecret})".parse[Term].get
        else
          m"${propertyInfo.name}.asOutput(isSecret = ${isSecret})".parse[Term].get

      Term.Assign(Term.Name(propertyInfo.name.value), argValue)
    }

    val argsCompanionWithArgsParams = argsCompanionApplyParams.map { param =>
      param.copy(default = Some(m"cls.${param.name.syntax}".parse[Term].get))
    }

    val derivedTypeclasses = {
      lazy val argsEncoderInstance =
        m"""|  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[$argsClassName] =
            |    besom.internal.ArgsEncoder.derived[$argsClassName]
            |""".stripMargin

      m"""|  given encoder(using besom.types.Context): besom.types.Encoder[$argsClassName] =
          |    besom.internal.Encoder.derived[$argsClassName]
          |$argsEncoderInstance""".stripMargin
    }

    m"""|object $argsClassName:
        |  def apply(
        |${argsCompanionApplyParams.map(arg => s"    ${arg.syntax}").mkString(",\n")}
        |  )(using besom.types.Context): $argsClassName =
        |    new $argsClassName(
        |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg.syntax}").mkString(",\n")}
        |    )
        |
        |  extension (cls: ${argsClassName}) def withArgs(
        |${argsCompanionWithArgsParams.map(arg => s"    ${arg.syntax}").mkString(",\n")}
        |  )(using besom.types.Context): $argsClassName =
        |    new $argsClassName(
        |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg.syntax}").mkString(",\n")}
        |    )
        |
        |$derivedTypeclasses
        |
        |${additionalCodecs.flatMap(_.codecs).mkString("\n")}
        |""".stripMargin.parse[Stat].get
  }
end ArgsClass
