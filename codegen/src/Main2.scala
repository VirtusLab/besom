import besom.codegen.SourceFile
import org.virtuslab.yaml.*
import org.virtuslab.yaml.Node.{MappingNode, ScalarNode}
import besom.codegen.scalameta.types
import besom.codegen.scalameta
import scala.util.Try
import besom.codegen.scalameta.interpolator.*

import scala.meta.*
import scala.meta.dialects.Scala33

extension [A](ie: Iterable[Either[Throwable, A]])
  def flattenWithFirstError: Either[Throwable, Iterable[A]] =
    val (error, pass) = ie.partitionMap(identity)
    if (error.nonEmpty) Left(error.head) else Right(pass)

case class CRD(spec: CRDSpec) derives YamlDecoder
case class CRDSpec(names: CRDNames, versions: Seq[CRDVersion]) derives YamlDecoder
case class CRDNames(singular: String, kind: String) derives YamlDecoder
case class CRDVersion(name: String, schema: CustomResourceValidation) derives YamlDecoder
case class CustomResourceValidation(openAPIV3Schema: JsonSchemaProps) derives YamlDecoder
case class JsonSchemaProps(
  `enum`: Option[List[String]],
  `type`: Option[DataTypeEnum],
  format: Option[String],
  required: Option[Set[String]],
  items: Option[JsonSchemaProps],
  properties: Option[Map[String, JsonSchemaProps]],
  additionalProperties: Option[Boolean | JsonSchemaProps]
) derives YamlDecoder

enum StringFormat:
  case date extends StringFormat
  case `date-time` extends StringFormat
  case password extends StringFormat
  case byte extends StringFormat
  case binary extends StringFormat

enum DataTypeEnum:
  case string extends DataTypeEnum
  case integer extends DataTypeEnum
  case number extends DataTypeEnum
  case `object` extends DataTypeEnum
  case boolean extends DataTypeEnum
  case array extends DataTypeEnum

object JsonSchemaProps:
  given YamlDecoder[DataTypeEnum] = YamlDecoder { case s @ ScalarNode(value, _) =>
    Try(DataTypeEnum.valueOf(value)).toEither.left
      .map(ConstructError.from(_, "enum DataTypeEnum", s))
  }
  given YamlDecoder[Option[JsonSchemaProps]] = YamlDecoder { case node =>
    YamlDecoder.forOption[JsonSchemaProps].construct(node)
  }
  given YamlDecoder[Map[String, JsonSchemaProps]] = YamlDecoder { case node =>
    YamlDecoder.forMap[String, JsonSchemaProps].construct(node)
  }
  given booleanOrJsonSchemaProps: YamlDecoder[Boolean | JsonSchemaProps] = YamlDecoder { case node =>
    YamlDecoder.forBoolean
      .construct(node)
      .left
      .flatMap(_ => summon[YamlDecoder[JsonSchemaProps]].construct(node))
  }

@main def main = {
  val testDirectory = os.pwd / ".out" / "a-test"
  val fileName      = "cert-manager.crds"
  os.remove.all(testDirectory)
  println(s"Remove all from $testDirectory")
  {
    for
      yamlFile <- Try(os.read(os.pwd / s"$fileName.yaml")).toEither
      rawCrds = yamlFile.split("---").toSeq
      crds <- rawCrds.map(_.as[CRD]).flattenWithFirstError
      _    <- crds.flatMap(createCaseClassVersions).map(sourceToFile(testDirectory, _)).flattenWithFirstError
    yield ()
  } match
    case Left(value) =>
      println("Error " + value)
    case Right(_) =>
      println("Success")
}

private def sourceToFile(mainDir: os.Path, sourceFile: SourceFile): Either[Throwable, Unit] = {
  val filePath = mainDir / sourceFile.filePath.osSubPath
  os.makeDir.all(filePath / os.up)
  Try(os.write(filePath, sourceFile.sourceCode, createFolders = true)).toEither
}

private def createCaseClassVersions(crd: CRD): Seq[SourceFile] =
  crd.spec.versions
    .flatMap { version =>
      val path = Seq(crd.spec.names.singular, version.name)
      createCaseClass(
        packagePath = path,
        className = crd.spec.names.kind,
        fields = version.schema.openAPIV3Schema.properties,
        required = version.schema.openAPIV3Schema.required.getOrElse(Set.empty)
      )
    }

private def createCaseClass(
  packagePath: Seq[String],
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

  val sourceFile = caseClassFile(packagePath, className, fieldsSyntax)
  sourceFile +: sourceFiles.flatten
}

private def parseJsonSchemaProps(packagePath: Seq[String]): (String, JsonSchemaProps) => (Type, Seq[SourceFile]) =
  case (fieldName, jsonSchema) if jsonSchema.`enum`.nonEmpty =>
    val enumList   = jsonSchema.`enum`.get
    val enumName   = fieldName.capitalize
    val sourceFile = enumFile(packagePath, enumName, enumList)
    val fieldType  = Type.Name(enumName)
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
    val required     = jsonSchema.required.getOrElse(Set.empty)
    val localization = packagePath :+ fieldName
    val fieldType    = Type.Select(scalameta.ref(localization.toList), Type.Name(fieldName.capitalize))

    (fieldType, createCaseClass(localization, fieldName.capitalize, jsonSchema.properties, required))

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

private def stringParseType(format: String): Type =
  StringFormat.valueOf(format) match
    case StringFormat.date =>
      Type.Select(scalameta.ref("java", "time"), Type.Name("LocalDate"))
    case StringFormat.`date-time` =>
      Type.Select(scalameta.ref("java", "time"), Type.Name("LocalDateTime"))
    case StringFormat.password =>
      types.String
    case StringFormat.byte =>
      types.Iterable(types.Byte)
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

private def caseClassFile(packagePath: Seq[String], className: String, fieldList: List[String]): SourceFile = {
  val createdClass =
    m"""|package ${packagePath.mkString(".")}
        |
        |final case class $className(
        |${fieldList.mkString(",\n")}
        |) derives besom.Decoder, besom.Encoder
        |""".stripMargin.parse[Source].get
  SourceFile(
    filePath = besom.codegen.FilePath(packagePath :+ s"$className.scala"),
    sourceCode = createdClass.syntax
  )
}

private def enumFile(packagePath: Seq[String], enumName: String, enumList: List[String]): SourceFile = {
  val createdClass =
    m"""|package ${packagePath.mkString(".")}
        |
        |enum $enumName derives besom.Decoder, besom.Encoder:
        |${enumList.map(e => s"  case ${Type.Name(e).syntax} extends $enumName").mkString("\n")}
        |""".stripMargin.parse[Source].get
  SourceFile(
    filePath = besom.codegen.FilePath(packagePath :+ s"$enumName.scala"),
    sourceCode = createdClass.syntax
  )
}

case class FieldNameWithType(fieldName: String, fieldType: scala.meta.Type)
