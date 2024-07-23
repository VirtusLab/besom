package besom.codegen.crd

import besom.codegen.scalameta.interpolator.*
import besom.codegen.scalameta.types
import besom.codegen.*
import org.virtuslab.yaml.*

import scala.meta.*
import scala.meta.dialects.Scala33
import scala.util.Try

object ClassGenerator:
  def main(args: Array[String]): Unit = {
    args.toList match {
      case yamlFilePath :: outputDirPath :: Nil =>
        val yamlFile  = yamlFilePath.toWorkingDirectoryPath
        val outputDir = outputDirPath.toWorkingDirectoryPath
        manageCrds(yamlFile, outputDir)
      case _ =>
        System.err.println(
          s"""|Unknown arguments: '${args.mkString(" ")}'
              |
              |Usage:
              |  <yamlFile> <outputDir> - Generate classes from path <yamlFile> and generate it to path <outputDir>
              |""".stripMargin
        )
        sys.exit(1)
    } match
      case Left(value) =>
        println("Error " + value)
      case Right(_) =>
        println("Success")
  }

  private def manageCrds(yamlFile: os.Path, outputDir: os.Path): Either[Throwable, Unit] = {
    os.remove.all(outputDir)
    println(s"Remove all from $outputDir")
    for
      yamlFile <- Try(os.read(yamlFile)).toEither
      rawCrds = yamlFile.split("---").toSeq
      crds <- rawCrds.map(_.as[CRD]).flattenWithFirstError
      _ <- crds
        .flatMap(c => createCaseClassVersions(c.spec))
        .map(sourceToFile(outputDir, _))
        .flattenWithFirstError
    yield ()
  }

  private def sourceToFile(mainDir: os.Path, sourceFile: SourceFile): Either[Throwable, Unit] = {
    val filePath = mainDir / sourceFile.filePath.osSubPath
    os.makeDir.all(filePath / os.up)
    Try(os.write(filePath, sourceFile.sourceCode, createFolders = true)).toEither
  }

  private def createCaseClassVersions(crd: CRDSpec): Seq[SourceFile] =
    crd.versions
      .flatMap { version =>
        val basePath = Seq(crd.names.singular, version.name)
        createCaseClass(
          packagePath = PackagePath(basePath),
          className = crd.names.kind,
          fields = version.schema.openAPIV3Schema.properties,
          required = version.schema.openAPIV3Schema.required.getOrElse(Set.empty)
        )
      }

  private def createCaseClass(
    packagePath: PackagePath,
    className: String,
    fields: Option[Map[String, JsonSchemaProps]],
    required: Set[String]
  ): Seq[SourceFile] = {
    val (classParams, sourceFiles) = fields
      .map(_.toList)
      .getOrElse(List.empty)
      .map((fieldName, jsonSchema) =>
        val (fieldType, sf) = parseJsonSchemaProps(packagePath)(fieldName, jsonSchema)
        (FieldNameWithType(fieldName, fieldType), sf)
      )
      .unzip

    val fieldsSyntax = classParams.map { fieldNameWithType =>
      val withOption = fieldNameWithType.copy(fieldType = toOption(fieldNameWithType, required))
      val termParam  = classField(withOption)
      termParam.syntax
    }
    val additionalCodecs =
      classParams.flatMap(fieldNameWithType => AdditionalCodecs.nameToValuesMap.get(fieldNameWithType.fieldType.syntax)).distinct
    val sourceFile = caseClassFile(packagePath.removeLastSegment, className, fieldsSyntax, additionalCodecs)
    sourceFile +: sourceFiles.flatten
  }

  private def parseJsonSchemaProps(packagePath: PackagePath): (String, JsonSchemaProps) => (Type, Seq[SourceFile]) =
    case (fieldName, jsonSchema) if jsonSchema.`enum`.nonEmpty =>
      val enumList   = jsonSchema.`enum`.get
      val enumName   = fieldName.capitalize
      val sourceFile = enumFile(packagePath, enumName, enumList)
      val fieldType  = Type.Select(scalameta.ref(packagePath.path.toList), Type.Name(enumName))

      (fieldType, Seq(sourceFile))
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.number) =>
      val fieldType = if jsonSchema.format.contains("float") then types.Float else types.Double

      (fieldType, Seq.empty)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.integer) =>
      val fieldType = if jsonSchema.format.contains("int64") then types.Long else types.Int

      (fieldType, Seq.empty)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.boolean) =>
      (types.Boolean, Seq.empty)

    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.string) =>
      val fieldType = jsonSchema.format.map(stringParseType).getOrElse(types.String)

      (fieldType, Seq.empty)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.array) && jsonSchema.items.isDefined =>
      val (classType, sourceFiles) = parseJsonSchemaProps(packagePath)(fieldName, jsonSchema.items.get)
      val fieldType                = types.Iterable(classType)

      (fieldType, sourceFiles)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.`object`) && jsonSchema.properties.nonEmpty =>
      val required  = jsonSchema.required.getOrElse(Set.empty)
      val className = fieldName.capitalize
      val fieldType = Type.Select(scalameta.ref(packagePath.path.toList), Type.Name(className))

      (fieldType, createCaseClass(packagePath.addPart(fieldName), className, jsonSchema.properties, required))
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.`object`) =>
      jsonSchema.additionalProperties match
        case Some(_: Boolean) | None =>
          val fieldType = types.Map(types.String, Type.Select(scalameta.ref("besom", "json"), Type.Name("JsValue")))
          (fieldType, Seq.empty)
        case Some(js: JsonSchemaProps) =>
          val (classType, sourceFiles) = parseJsonSchemaProps(packagePath)(fieldName, js)
          val fieldType                = types.Map(types.String, classType)
          (fieldType, sourceFiles)

    case (fieldName, jsonSchema) =>
      println(s"Problem when decoding `$fieldName` field with type ${jsonSchema.`type`}, create Map[String, JsValue]")
      val fieldType = types.Map(types.String, Type.Select(scalameta.ref("besom", "json"), Type.Name("JsValue")))

      (fieldType, Seq.empty)
  end parseJsonSchemaProps

  enum StringFormat:
    case date extends StringFormat
    case `date-time` extends StringFormat
    case password extends StringFormat
    case byte extends StringFormat
    case binary extends StringFormat

  private def stringParseType(format: String): Type =
    StringFormat.valueOf(format) match
      case StringFormat.date =>
        Type.Select(scalameta.ref("java", "time"), Type.Name("LocalDate"))
      case StringFormat.`date-time` =>
        Type.Select(scalameta.ref("java", "time"), Type.Name("LocalDateTime"))
      case StringFormat.password =>
        types.String
      case StringFormat.byte =>
        types.String
      case StringFormat.binary =>
        types.String

  private def classField(fieldNameWithType: FieldNameWithType): Term.Param =
    Term.Param(
      mods = List.empty,
      name = scala.meta.Name(fieldNameWithType.fieldName),
      decltpe = Some(fieldNameWithType.fieldType),
      default = None
    )

  private def toOption(fieldNameWithType: FieldNameWithType, required: Set[String]): Type =
    if required(fieldNameWithType.fieldName) then fieldNameWithType.fieldType else types.Option(fieldNameWithType.fieldType)

  private def caseClassFile(
    packagePath: PackagePath,
    className: String,
    fieldList: List[String],
    additionalCodecs: List[besom.codegen.crd.AdditionalCodecs]
  ): SourceFile = {
    val companionObject = Option.when(additionalCodecs.nonEmpty) {
      m"""|object $className:
          |${additionalCodecs.flatMap(_.codecs).mkString("\n")}
          |""".stripMargin.parse[Stat].get
    }

    val createdClass =
      m"""|package ${packagePath.path.mkString(".")}
          |
          |final case class $className(
          |${fieldList.mkString(",\n")}
          |) derives besom.Decoder, besom.Encoder
          |
          |${companionObject.getOrElse("")}
          |""".stripMargin.parse[Source].get

    SourceFile(
      filePath = besom.codegen.FilePath(packagePath.path :+ s"$className.scala"),
      sourceCode = createdClass.syntax
    )
  }

  private def enumFile(packagePath: PackagePath, enumName: String, enumList: List[String]): SourceFile = {
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

  private case class FieldNameWithType(fieldName: String, fieldType: scala.meta.Type)

  case class PackagePath(baseSegments: Seq[String], segments: Seq[String] = Seq.empty):
    def addPart(part: String): PackagePath = PackagePath(baseSegments, part +: segments)
    def path: Seq[String]                  = baseSegments ++ segments.reverse
    def removeLastSegment: PackagePath     = PackagePath(baseSegments, segments.drop(1))
  object PackagePath:
    def apply(base: Seq[String]): PackagePath = new PackagePath(base)
end ClassGenerator
