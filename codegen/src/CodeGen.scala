package besom.codegen

import java.nio.file.{Path, Paths}

import scala.util.matching.Regex

import scala.meta._
import scala.meta.dialects.Scala33
import scala.util.control.NonFatal

import besom.codegen.metaschema._

object CodeGen {
  val baseApiPackagePath = PackagePath(Seq("besom", "api"))

  val commonImportedIdentifiers = Seq(
    "besom.util.NotProvided",
    "besom.internal.Output",
    "besom.internal.Context",
    "besom.types.PulumiArchive",
    "besom.types.PulumiAsset",
    "besom.types.PulumiAny",
    "besom.types.PulumiJson"
  )

  private[codegen] case class TypeTokenStruct(providerName: String, packageSuffix: String, typeName: String)

  class TypeMapper(
    moduleToPackage: String => String,
    enumTypeTokens: Set[String],
    objectTypeTokens: Set[String],
    resourceTypeTokens: Set[String],
    moduleFormat: Regex
  ) {
    def parseTypeToken(typeToken: String): TypeTokenStruct = {
      val Array(providerName, modulePortion, typeName) = typeToken.split(":")
      val moduleName = modulePortion match {
        case moduleFormat(name) => name
      }
      TypeTokenStruct(
        providerName = moduleToPackage(providerName),
        packageSuffix = moduleToPackage(moduleName),
        typeName = typeName
      )
    }

    def scalaTypeFromNonResourceTypeToken(escapedTypeToken: String, isResource: Boolean, asArgsType: Boolean) = {
      val typeToken = escapedTypeToken.replace("%2F", "/") // TODO: Proper URL unescaping ?
      val typeTokenStruct = parseTypeToken(typeToken)
      val isObjectType = objectTypeTokens.contains(typeToken)
      val isEnumType = enumTypeTokens.contains(typeToken)
      val typeNameSuffix = if (asArgsType && !isEnumType) "Args" else ""
      val typeName = s"${typeTokenStruct.typeName}${typeNameSuffix}"
      val typeKindSubpackage =
        if (isResource) ""
        else if (isEnumType) "enums"
        else if (asArgsType) "inputs" else "outputs"
      val packagePath = baseApiPackagePath.resolve(typeTokenStruct.providerName).resolve(typeTokenStruct.packageSuffix).resolve(typeKindSubpackage)
      val packageRef = packagePath.pathParts.toList.foldLeft[Term.Ref](q"_root_")((acc, name) => Term.Select(acc, Term.Name(name)))

      t"${packageRef}.${Type.Name(typeName)}"
    }

    def asScalaType(typeRef: TypeReference, asArgsType: Boolean): Type = typeRef match {
      case BooleanType => t"Boolean"
      case StringType => t"String"
      case IntegerType => t"Int"
      case NumberType => t"Double"
      case ArrayType(elemType) => t"List[${asScalaType(elemType, asArgsType)}]"
      case MapType(elemType) => t"Map[String, ${asScalaType(elemType, asArgsType)}]"
      case unionType: UnionType =>
        unionType.oneOf.map(asScalaType(_, asArgsType)).reduce{ (t1, t2) => t"$t1 | $t2"}
      case namedType: NamedType =>
        namedType.typeUri match {
          case "pulumi.json#/Archive" =>
            t"besom.types.PulumiArchive"
          case "pulumi.json#/Asset" =>
            t"besom.types.PulumiAsset"
          case "pulumi.json#/Any" =>
            t"besom.types.PulumiAny"
          case "pulumi.json#/Json" =>
            t"besom.types.PulumiJson"

          case typeUri =>
            // Example URIs:
            // "/provider/vX.Y.Z/schema.json#/types/pulumi:type:token"
            // #/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject
            // "aws:iam/documents:PolicyDocument"

            val (fileUri, typePath) = typeUri.split("#") match {
              case Array(typePath) => ("", typePath)
              case Array(fileUri, typePath) => (fileUri, typePath)
              case _ => throw new Exception(s"Unexpected type URI format: ${typeUri}") 
            }

            assert(fileUri == "", s"Invalid type URI: $typeUri - referencing other schemas is not supported")

            typePath match {
              case s"/types/${token}" =>
                scalaTypeFromNonResourceTypeToken(token, isResource = false, asArgsType = asArgsType)
              case s"/resources/${token}" =>
                scalaTypeFromNonResourceTypeToken(token, isResource = true, asArgsType = asArgsType)
              case s"/${rest}" =>
                throw new Exception(s"Invalid type reference: ${typeRef}")
              case token =>
                if (objectTypeTokens.contains(token) || enumTypeTokens.contains(token)) {
                  println(s"Warning: type reference: ${typeRef} has an invalid format - assuming '#/types/' prefix")
                  scalaTypeFromNonResourceTypeToken(token, isResource = false, asArgsType = asArgsType)
                } else {
                  println(s"Warning: type reference: ${typeRef} has an invalid format and is not defined in the current schema - using underlying primitive type as fallback")
                  asScalaType(namedType.`type`.get, asArgsType = asArgsType)
                }
            }
        }
    }
  }

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper): Type = typeMapper.asScalaType(typeRef, asArgsType)
  }

  def sourcesFromPulumiPackage(pulumiPackage: PulumiPackage, besomVersion: String): Seq[SourceFile] = {
    val enumTypeTokensBuffer = collection.mutable.ListBuffer.empty[String]
    val objectTypeTokensBuffer = collection.mutable.ListBuffer.empty[String]
    
    pulumiPackage.types.foreach {
      case (typeToken, _: EnumTypeDefinition) => enumTypeTokensBuffer += typeToken  
      case (typeToken, _: ObjectTypeDefinition) => objectTypeTokensBuffer += typeToken  
    }

    val enumTypeTokens = enumTypeTokensBuffer.toSet
    val objectTypeTokens = objectTypeTokensBuffer.toSet
    val resourceTypeTokens = pulumiPackage.resources.keySet

    implicit val typeMapper: TypeMapper = new TypeMapper(
      moduleToPackage = pulumiPackage.language.java.packages.withDefault(x => x),
      enumTypeTokens = enumTypeTokens,
      objectTypeTokens = objectTypeTokens,
      resourceTypeTokens = resourceTypeTokens,
      moduleFormat = pulumiPackage.meta.moduleFormat.r
    )

    val scalaSources = (
      sourceFilesForProviderResource(pulumiPackage) ++
      sourceFilesForNonResourceTypes(pulumiPackage) ++
      sourceFilesForCustomResources(pulumiPackage)
    ).map{ sourceFile => sourceFile.copy(relativePath = Paths.get("src").resolve(sourceFile.relativePath)) }
    
    scalaSources ++ Seq(
      projectConfigFile(
        pulumiPackageName = pulumiPackage.name,
        besomVersion = besomVersion
      ),
      resourcePluginMetadataFile(
        pluginName = pulumiPackage.name,
        pluginVersion = pulumiPackage.version
      ),
    )
  }

  def projectConfigFile(pulumiPackageName: String, besomVersion: String): SourceFile = {
    // TODO use original package version from the schema as publish.version?

    val fileContent =
      s"""|//> using scala "3.2.2"
          |//> using lib "org.virtuslab::besom-core:${besomVersion}"
          |
          |//> using resourceDir "resources"
          |
          |//> using publish.organization "org.virtuslab"
          |//> using publish.name "besom-${pulumiPackageName}"
          |//> using publish.version "${besomVersion}"
          |""".stripMargin

    val filePath = Paths.get("project.scala")

    SourceFile(relativePath = filePath, sourceCode = fileContent)
  }

  def resourcePluginMetadataFile(pluginName: String, pluginVersion: String): SourceFile = {
    val fileContent =
      s"""|{
          |  "resource": true,
          |  "name": "${pluginName}",
          |  "version": "${pluginVersion}"
          |}
          |""".stripMargin
    val filePath = Paths.get("resources", "plugin.json")

    SourceFile(relativePath = filePath, sourceCode = fileContent)
  } 

  def sourceFilesForProviderResource(pulumiPackage: PulumiPackage)(implicit typeMapper: TypeMapper): Seq[SourceFile] = {
    val asPackageName = pulumiPackage.language.java.packages.withDefault(x => x)

    val providerName = pulumiPackage.name
    val providerPackageSuffix = asPackageName(providerName)
    val packagePath = baseApiPackagePath.resolve(providerPackageSuffix)
    sourceFilesForResource(
      resourceName = "Provider",
      resourceDefinition = pulumiPackage.provider,
      packagePath = packagePath,
      typeToken = s"pulumi:provider:${pulumiPackage.name}",
      isProvider = true
    )
  }


  def sourceFilesForNonResourceTypes(pulumiPackage: PulumiPackage)(implicit typeMapper: TypeMapper): Seq[SourceFile] = {
    val asPackageName = pulumiPackage.language.java.packages.withDefault(x => x)
    
    pulumiPackage.types.flatMap { case (typeToken, typeDefinition) =>
      val typeTokenStruct = typeMapper.parseTypeToken(typeToken)
      val providerName = typeTokenStruct.providerName
      val typePackageSuffix = typeTokenStruct.packageSuffix
      val typeName = typeTokenStruct.typeName

      val providerPackageSuffix = asPackageName(providerName)
      val packagePathPrefix = baseApiPackagePath.resolve(providerPackageSuffix).resolve(typePackageSuffix)

      typeDefinition match {
        case enumDef: EnumTypeDefinition => sourceFilesForEnum(enumName = typeName, enumDefinition = enumDef, packagePathPrefix = packagePathPrefix)
        case objectDef: ObjectTypeDefinition => sourceFilesForObjectType(typeName = typeName, objectTypeDefinition = objectDef, packagePathPrefix = packagePathPrefix)
      }
    }.toSeq
  }

  def sourceFilesForEnum(enumName: String, enumDefinition: EnumTypeDefinition, packagePathPrefix: PackagePath): Seq[SourceFile] = {
    val packagePath = packagePathPrefix.resolve("enums")

    val imports = makeImportStatements(Seq(
      "besom.internal.Decoder"
    ))

    val superclass = enumDefinition.`type` match {
      case BooleanType => "besom.internal.BooleanEnum"
      case IntegerType => "besom.internal.IntegerEnum"
      case NumberType => "besom.internal.NumberEnum"
      case StringType => "besom.internal.StringEnum"
    }

    val enumCases = enumDefinition.`enum`.map { valueDefinition =>
      val caseRawName = valueDefinition.name.getOrElse(valueDefinition.value)
      val caseName = Term.Name(caseRawName).syntax
      s"case ${caseName}"
    } 
    
    val fileContent =
      s"""|package ${packagePath.packageName}
          |
          |${imports}
          |
          |enum ${enumName} extends ${superclass} derives Decoder:
          |${enumCases.map(arg => s"  ${arg}").mkString("\n")}
          |""".stripMargin

    Seq(SourceFile(
      packagePath.filePath.resolve(s"${enumName}.scala"),
      fileContent
    ))
  }

  def sourceFilesForObjectType(typeName: String, objectTypeDefinition: ObjectTypeDefinition, packagePathPrefix: PackagePath)(implicit typeMapper: TypeMapper): Seq[SourceFile] = {
    val baseClassName = Type.Name(typeName).syntax
    val baseClassFileName = s"${typeName}.scala"
    val argsClassName = Type.Name(s"${typeName}Args").syntax
    val argsClassFileName = s"${typeName}Args.scala"
    
    val baseFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.internal.Decoder"
    ))

    val argsFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.internal.Encoder",
      "besom.internal.ArgsEncoder"
    ))

    val baseClassParams = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      val fieldBaseType = propertyDefinition.typeReference.asScalaType()
      val isRequired = objectTypeDefinition.required.contains(propertyName)
      val fieldType = if (isRequired) fieldBaseType else t"Option[$fieldBaseType]"
      Term.Param(
        mods = List.empty,
        name = Term.Name(propertyName),
        decltpe = Some(fieldType),
        default = None
      ).syntax
    }

    val argsClassParams = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      makeArgsClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsCompanionApplyParams = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      val isRequired = objectTypeDefinition.required.contains(propertyName)
      makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition, isRequired)
    }

    val argsCompanionApplyBodyArgs = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition)
    }

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val baseClass =
      s"""|case class $baseClassName(
          |${baseClassParams.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives Decoder""".stripMargin

    val argsClass =
      s"""|case class $argsClassName(
          |${argsClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) derives Encoder, ArgsEncoder""".stripMargin

    val argsCompanion =
      s"""|object $argsClassName:
          |  def apply(
          |${argsCompanionApplyParams.map(param => s"    ${param}").mkString(",\n")}
          |  )(using Context): $argsClassName =
          |    new $argsClassName(
          |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg}").mkString(",\n")}
          |    )""".stripMargin

    val baseClassPackagePath = packagePathPrefix.resolve("outputs")
    val argsClassPackagePath = packagePathPrefix.resolve("inputs")

    val baseClassFileContent =
      s"""|package ${baseClassPackagePath.packageName}
          |
          |${baseFileImports}
          |
          |${baseClassComment}
          |${baseClass}
          |
          |""".stripMargin

    val argsClassFileContent =
      s"""|package ${argsClassPackagePath.packageName} 
          |
          |${argsFileImports}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin

    Seq(
      SourceFile(
        baseClassPackagePath.filePath.resolve(baseClassFileName),
        baseClassFileContent
      ),
      SourceFile(
        argsClassPackagePath.filePath.resolve(argsClassFileName),
        argsClassFileContent
      )
    )
  }

  def sourceFilesForCustomResources(pulumiPackage: PulumiPackage)(implicit typeMapper: TypeMapper): Seq[SourceFile] = {
    val asPackageName = pulumiPackage.language.java.packages.withDefault(x => x)

    pulumiPackage.resources.collect { case (typeToken, resourceDefinition) if !resourceDefinition.isOverlay =>
      
      val typeTokenStruct = typeMapper.parseTypeToken(typeToken)

      val providerName = typeTokenStruct.providerName
      val typePackageSuffix = typeTokenStruct.packageSuffix
      val resourceName = typeTokenStruct.typeName

      val providerPackageSuffix = asPackageName(providerName)
      val packagePath = baseApiPackagePath.resolve(providerPackageSuffix).resolve(typePackageSuffix)
      
      sourceFilesForResource(
        resourceName = resourceName,
        resourceDefinition = resourceDefinition,
        packagePath = packagePath,
        typeToken = typeToken,
        isProvider = false,
      )
    }.toSeq.flatten
  }

  def sourceFilesForResource(resourceName: String, resourceDefinition: ResourceDefinition, packagePath: PackagePath, typeToken: String, isProvider: Boolean)(implicit typeMapper: TypeMapper): Seq[SourceFile] = {
    val baseClassName = Type.Name(resourceName).syntax
    val baseClassFileName = s"${resourceName}.scala"
    val argsClassName = Type.Name(s"${resourceName}Args").syntax
    val argsClassFileName = s"${resourceName}Args.scala"
    val factoryMethodName = Term.Name(decapitalize(resourceName)).syntax

    val baseFileImports = {
      val conditionalIdentifiers =
        if (isProvider)
          Seq("besom.internal.ProviderResource")
        else
          Seq("besom.internal.CustomResource")
      val unconditionalIdentifiers = Seq(
        "besom.internal.ResourceDecoder",
        "besom.internal.CustomResourceOptions",
        "besom.util.NonEmptyString",
      )
      makeImportStatements(commonImportedIdentifiers ++ conditionalIdentifiers ++ unconditionalIdentifiers)
    }

    val argsFileImports = {
      val conditionalIdentifiers =
        if (isProvider)
          Seq("besom.internal.ProviderArgsEncoder")
        else
          Seq("besom.internal.ArgsEncoder")
      makeImportStatements(commonImportedIdentifiers ++ conditionalIdentifiers)
    }

    val resourceBaseProperties = Seq(
      "urn" -> PropertyDefinition(typeReference = StringType),
      "id" -> PropertyDefinition(typeReference = StringType)
    )

    val resourceProperties = resourceBaseProperties ++ resourceDefinition.properties

    val baseClassParams = resourceProperties.map { case (propertyName, propertyDefinition) =>
      makeResourceClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsClassParams = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsCompanionApplyParams = resourceDefinition.inputProperties.collect {
      case (propertyName, propertyDefinition) if propertyDefinition.const.isEmpty =>
        val isRequired = resourceDefinition.required.contains(propertyName)
        makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition, isRequired = isRequired)
    }

    val argsCompanionApplyBodyArgs = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition)
    }

    val packageDecl = s"package ${packagePath.packageName}"

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val resourceBaseClass = if (isProvider) "ProviderResource" else "CustomResource"

    val baseClass =
      s"""|case class $baseClassName(
          |${baseClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives ResourceDecoder""".stripMargin

    // the type has to match pulumi's resource type schema, ie kubernetes:core/v1:Pod
    val typ = Lit.String(typeToken)

    val factoryMethod =
      s"""|def $factoryMethodName(using ctx: Context)(
          |  name: NonEmptyString,
          |  args: $argsClassName,
          |  opts: CustomResourceOptions = CustomResourceOptions()
          |): Output[$baseClassName] = 
          |  ctx.registerResource[$baseClassName, $argsClassName](${typ}, name, args, opts)
          |""".stripMargin

    val argsEncoderClassName = if (isProvider) "ProviderArgsEncoder" else "ArgsEncoder"

    val argsClass =
      s"""|case class $argsClassName(
          |${argsClassParams.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives ${argsEncoderClassName}""".stripMargin

    val argsCompanion =
      s"""|object $argsClassName:
          |  def apply(
          |${argsCompanionApplyParams.map(arg => s"    ${arg}").mkString(",\n")}
          |  )(using Context): $argsClassName =
          |    new $argsClassName(
          |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg}").mkString(",\n")}
          |    )""".stripMargin

    val baseClassFileContent =
      s"""|$packageDecl
          |
          |${baseFileImports}
          |
          |${baseClassComment}
          |${baseClass}
          |
          |${factoryMethod}
          |""".stripMargin

    val argsClassFileContent =
      s"""|$packageDecl
          |
          |${argsFileImports}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin

    Seq(
      SourceFile(
        packagePath.filePath.resolve(baseClassFileName),
        baseClassFileContent
      ),
      SourceFile(
        packagePath.filePath.resolve(argsClassFileName),
        argsClassFileContent
      )
    )
  }

  private def makeResourceClassParam(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper) = {
    val fieldBaseType = property.typeReference.asScalaType()
    val fieldType = t"Output[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(propertyName),
      decltpe = Some(fieldType),
      default = None
    ).syntax
  }

  private def makeArgsClassParam(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper) = {
    val fieldBaseType = property.typeReference.asScalaType(asArgsType = true)
    val fieldType = t"Output[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(propertyName),
      decltpe = Some(fieldType),
      default = None
    ).syntax
  }

  private def makeArgsCompanionApplyParam(propertyName: String, property: PropertyDefinition, isRequired: Boolean)(implicit typeMapper: TypeMapper) = {
    val requiredParamType = property.typeReference match {
      case MapType(additionalProperties) =>
        val valueType = additionalProperties.asScalaType(asArgsType = true)
        t"""Map[String, $valueType] | Map[String, Output[$valueType]] | Output[Map[String, $valueType]]"""
      case tpe =>
        val baseType = tpe.asScalaType(asArgsType = true)
        t"""$baseType | Output[$baseType]"""
    }

    val paramType =
      if (isRequired) requiredParamType
      else t"""$requiredParamType | NotProvided"""

    val default = property.default match {
      case Some(defaultValue) =>
        Some(constValueAsCode(defaultValue))
      case None =>
        if (isRequired) None else Some(q"NotProvided")
    }

    Term.Param(
      mods = List.empty,
      name = Term.Name(propertyName),
      decltpe = Some(paramType),
      default = default
    ).syntax
  }

  private def makeArgsCompanionApplyBodyArg(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper) = {
    val fieldTermName = Term.Name(propertyName)
    val isSecret = Lit.Boolean(property.secret)
    val argValue = property.const match {
      case Some(constValue) =>
        q"Output(${constValueAsCode(constValue)})"
      case None =>
        property.typeReference match {
          case MapType(_) =>
            q"${fieldTermName}.asOutputMap(isSecret = ${isSecret})"
          case _ =>
            q"${fieldTermName}.asOutput(isSecret = ${isSecret})"
        }
    }
    Term.Assign(fieldTermName, argValue).syntax
  }

  private def constValueAsCode(constValue: ConstValue) = constValue match {
    case StringConstValue(value) =>
      Lit.String(value)
    case BooleanConstValue(value) =>
      Lit.Boolean(value)
  }

  private def decapitalize(s: String) = s(0).toLower.toString ++ s.substring(1, s.length)

  private def makeImportStatements(importedIdentifiers: Seq[String]) =
    importedIdentifiers.map(id => s"import $id").mkString("\n")
}

case class PackagePath(pathParts: Seq[String]) {
  def packageName: String = pathParts.mkString(".")
  def filePath: Path = Paths.get(pathParts.head, pathParts.tail.toArray: _*)

  def resolve(name: String) = PackagePath(pathParts ++ name.split("\\.").filter(_.nonEmpty))
}

case class SourceFile(
  relativePath: Path,
  sourceCode: String
)


// TODOs:
// * Can providerPackageSuffix be multipart with doty inside?