package besom.codegen.crd

import besom.codegen.scalameta.interpolator.*
import besom.codegen.*
import scala.meta.*
import scala.meta.dialects.Scala33

object EnumClass:
  def enumFile(packagePath: PackagePath, enumName: String, enumList: List[String]): SourceFile = {
    val companionObject =
      m"""|object $enumName:
          |${AdditionalCodecs.enumCodecs(enumName).mkString("\n")}
          |""".stripMargin.parse[Stat].get

    val createdClass =
      m"""|package ${packagePath.path.mkString(".")}
          |
          |enum $enumName:
          |${enumList.map(e => s"  case ${Type.Name(e).syntax} extends $enumName").mkString("\n")}
          |
          |$companionObject
          |""".stripMargin.parse[Source].get
    SourceFile(
      filePath = besom.codegen.FilePath(packagePath.path :+ s"$enumName.scala"),
      sourceCode = createdClass.syntax
    )
  }
