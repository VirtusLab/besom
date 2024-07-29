package besom.codegen.crd

import besom.codegen.scalameta.types
import besom.codegen.*
import org.virtuslab.yaml.*

import scala.meta.*
import scala.util.Try

object ClassGenerator:
  private val jsValueType = Type.Select(scalameta.ref("besom", "json"), Type.Name("JsValue"))

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
        // we are only interested in the spec field
        version.schema.openAPIV3Schema.properties.flatMap(_.get("spec")) match
          case Some(spec) =>
            parseJsonSchema(
              packagePath = PackagePath(basePath),
              className = crd.names.kind,
              parentJsonSchema = spec
            )
          case None =>
            throw Exception("Spec field not found in openAPIV3Schema properties")
      }

  private def parseJsonSchema(packagePath: PackagePath, className: String, parentJsonSchema: JsonSchemaProps): Seq[SourceFile] = {
    val (classFields, sourceFileAcc) =
      parentJsonSchema.properties
        .map(_.toList)
        .getOrElse(List.empty)
        .map(parseJsonSchemaProperty(packagePath, parentJsonSchema)(_, _))
        .unzip
    val sourceFile =
      ArgsClass.makeArgsClassSourceFile(
        argsClassName = Type.Name(className),
        packagePath = packagePath.removeLastSegment,
        properties = classFields,
        additionalCodecs = classFields
          .flatMap(fieldNameWithType => AdditionalCodecs.nameToValuesMap.get(fieldNameWithType.baseType))
          .distinct
      )

    sourceFile +: sourceFileAcc.flatten
  }

  private def parseJsonSchemaProperty(
    packagePath: PackagePath,
    parentJsonSchema: JsonSchemaProps
  ): (String, JsonSchemaProps) => (FieldTypeInfo, Seq[SourceFile]) =
    case (fieldName, jsonSchema) if jsonSchema.`enum`.nonEmpty =>
      enumType(packagePath, fieldName, jsonSchema, parentJsonSchema)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.number) =>
      numberType(fieldName, jsonSchema, parentJsonSchema)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.integer) =>
      integerType(fieldName, jsonSchema, parentJsonSchema)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.boolean) =>
      booleanType(fieldName, jsonSchema, parentJsonSchema)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.string) =>
      stringType(fieldName, jsonSchema, parentJsonSchema)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.array) && jsonSchema.items.isDefined =>
      arrayType(packagePath, fieldName, jsonSchema, parentJsonSchema)
    case (fieldName, jsonSchema) if jsonSchema.`type`.contains(DataTypeEnum.`object`) =>
      objectType(packagePath, fieldName, jsonSchema, parentJsonSchema)
    case (fieldName, jsonSchema) =>
      defaultType(fieldName, jsonSchema, parentJsonSchema)

  private def enumType(
    packagePath: PackagePath,
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    val enumList   = jsonSchema.`enum`.get
    val enumName   = fieldName.capitalize
    val sourceFile = EnumClass.enumFile(packagePath, enumName, enumList)
    val fieldTypeInfo =
      FieldTypeInfo(
        name = fieldName,
        baseType = Type.Select(scalameta.ref(packagePath.path.toList), Type.Name(enumName)),
        jsonSchema = jsonSchema,
        parentJsonSchema = parentJsonSchema
      )
    (fieldTypeInfo, Seq(sourceFile))
  }

  private def numberType(
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    val baseType = jsonSchema.format.map(NumberFormat.valueOf) match
      case Some(NumberFormat.float)         => types.Float
      case Some(NumberFormat.double) | None => types.Double

    val fieldTypeInfo =
      FieldTypeInfo(
        name = fieldName,
        baseType = baseType,
        jsonSchema = jsonSchema,
        parentJsonSchema = parentJsonSchema
      )
    (fieldTypeInfo, Seq.empty)
  }

  private def integerType(
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    val baseType = jsonSchema.format.map(IntegerFormat.valueOf) match
      case Some(IntegerFormat.int64)        => types.Long
      case Some(IntegerFormat.int32) | None => types.Int

    val fieldTypeInfo =
      FieldTypeInfo(
        name = fieldName,
        baseType = baseType,
        jsonSchema = jsonSchema,
        parentJsonSchema = parentJsonSchema
      )
    (fieldTypeInfo, Seq.empty)
  }

  private def booleanType(
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    val fieldTypeInfo =
      FieldTypeInfo(
        name = fieldName,
        baseType = types.Boolean,
        jsonSchema = jsonSchema,
        parentJsonSchema = parentJsonSchema
      )
    (fieldTypeInfo, Seq.empty)
  }

  private def stringType(
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    val (isSecret, baseType) = jsonSchema.format.map(stringParseType).getOrElse((false, types.String))
    val fieldTypeInfo =
      FieldTypeInfo(
        name = fieldName,
        baseType = baseType,
        jsonSchema = jsonSchema,
        parentJsonSchema = parentJsonSchema,
        isSecret = isSecret
      )
    (fieldTypeInfo, Seq.empty)
  }

  private def stringParseType(format: String): (Boolean, Type) =
    StringFormat.valueOf(format) match
      case StringFormat.date =>
        val `type` = Type.Select(scalameta.ref("java", "time"), Type.Name("LocalDate"))
        (false, `type`)
      case StringFormat.`date-time` =>
        val `type` = Type.Select(scalameta.ref("java", "time"), Type.Name("LocalDateTime"))
        (false, `type`)
      case StringFormat.password =>
        (true, types.String)
      case StringFormat.byte =>
        (false, types.String)
      case StringFormat.binary =>
        (false, types.String)

  private def arrayType(
    packagePath: PackagePath,
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    val (classType, sourceFiles) =
      parseJsonSchemaProperty(packagePath, jsonSchema)(fieldName, jsonSchema.items.get)
    val fieldTypeInfo =
      FieldTypeInfo(
        name = fieldName,
        baseType = types.Iterable(classType.baseType),
        jsonSchema = jsonSchema,
        parentJsonSchema = parentJsonSchema
      )
    (fieldTypeInfo, sourceFiles)
  }

  private def objectType(
    packagePath: PackagePath,
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    (jsonSchema.properties, jsonSchema.additionalProperties) match
      case (Some(_), _) =>
        val className = fieldName.capitalize
        val fieldTypeInfo =
          FieldTypeInfo(
            name = fieldName,
            baseType = Type.Select(scalameta.ref(packagePath.path.toList), Type.Name(className)),
            jsonSchema = jsonSchema,
            parentJsonSchema = parentJsonSchema
          )
        (fieldTypeInfo, parseJsonSchema(packagePath.addPart(fieldName), className, jsonSchema))
      case (_, Some(_: Boolean) | None) =>
        val fieldTypeInfo =
          FieldTypeInfo(
            name = fieldName,
            baseType = types.Map(types.String, jsValueType),
            jsonSchema = jsonSchema,
            parentJsonSchema = parentJsonSchema
          )
        (fieldTypeInfo, Seq.empty)
      case (_, Some(js: JsonSchemaProps)) =>
        val (classType, sourceFiles) =
          parseJsonSchemaProperty(packagePath, jsonSchema)(fieldName, js)
        val fieldTypeInfo =
          FieldTypeInfo(
            name = fieldName,
            baseType = types.Map(types.String, classType.baseType),
            jsonSchema = jsonSchema,
            parentJsonSchema = parentJsonSchema,
            isSecret = classType.isSecret
          )
        (fieldTypeInfo, sourceFiles)
  }

  private def defaultType(
    fieldName: String,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps
  ): (FieldTypeInfo, Seq[SourceFile]) = {
    println(s"Problem when decoding `$fieldName` field with type ${jsonSchema.`type`}, create Map[String, JsValue]")
    val fieldTypeInfo =
      FieldTypeInfo(
        name = fieldName,
        baseType = types.Map(types.String, jsValueType),
        jsonSchema = jsonSchema,
        parentJsonSchema = parentJsonSchema
      )
    (fieldTypeInfo, Seq.empty)
  }
end ClassGenerator

case class PackagePath(baseSegments: Seq[String], segments: Seq[String] = Seq.empty):
  def addPart(part: String): PackagePath = PackagePath(baseSegments, part +: segments)
  def path: Seq[String]                  = baseSegments ++ segments.reverse
  def removeLastSegment: PackagePath     = PackagePath(baseSegments, segments.drop(1))
object PackagePath:
  def apply(base: Seq[String]): PackagePath = new PackagePath(base)
