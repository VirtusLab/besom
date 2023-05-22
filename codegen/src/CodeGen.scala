package besom.codegen

import java.nio.file.{Path, Paths}

import scala.util.matching.Regex

import scala.meta._
import scala.meta.dialects.Scala33
import scala.util.control.NonFatal

import besom.codegen.metaschema._

object CodeGen {
  val basePackage = "besom.api"

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

    def asScalaType(typeRef: TypeReference, asArgsType: Boolean = false): Type = typeRef match {
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

            val (escapedTypeToken, isResource) = typePath match {
              case s"/types/${token}" => (token, false)
              case s"/resources/${token}" => (token, true)
              case _ =>
                //  TODO does typePath need URL escaping in this case too?
                if (objectTypeTokens.contains(typePath) || enumTypeTokens.contains(typePath))
                  (typePath, false)
                else if (resourceTypeTokens.contains(typePath))
                  (typePath, true)
                else
                  return asScalaType(namedType.`type`.get, asArgsType = asArgsType)
            }
            val typeToken = escapedTypeToken.replace("%2F", "/") // TODO: Proper URL unescaping ?
            val typeTokenStruct = parseTypeToken(typeToken)
            val typeNameSuffix = if (asArgsType && !enumTypeTokens.contains(typeToken)) "Args" else ""
            val typeName = s"${typeTokenStruct.typeName}${typeNameSuffix}"
            val resourcePackagePart = if (isResource) Seq("resources") else Seq.empty
            val packageParts = basePackage.split("\\.") ++ Seq(typeTokenStruct.providerName) ++ typeTokenStruct.packageSuffix.split("\\.") ++ resourcePackagePart
            val packageRef = packageParts.toList.foldLeft[Term.Ref](q"_root_")((acc, name) => Term.Select(acc, Term.Name(name)))

            t"${packageRef}.${Type.Name(typeName)}"

        }
    }
  }

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper): Type = typeMapper.asScalaType(typeRef, asArgsType)
  }

  def sourcesFromPulumiPackage(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
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

    Seq(
      sourceFileForBuildDefinition(providerName = pulumiPackage.name),
      sourceFileFromPulumiProvider(pulumiPackage)
    ) ++
    sourceFilesForNonResourceTypes(pulumiPackage) ++
    sourceFilesForCustomResources(pulumiPackage)
  }

  def sourceFileForBuildDefinition(providerName: String): SourceFile = {
    val fileContent =
      s"""|//> using scala "3.2.2"
          |//> using lib "org.virtuslab::besom-core:0.0.1-SNAPSHOT"
          |
          |//> using publish.organization "org.virtuslab"
          |//> using publish.name "besom-kubernetes"
          |//> using publish.version "0.0.1-SNAPSHOT"
          |""".stripMargin

    val pathParts = basePackage.split("\\.") ++ Seq(providerName, "project.scala")
    val filePath = Paths.get(pathParts.head, pathParts.tail.toArray: _*)

    SourceFile(relativePath = filePath, sourceCode = fileContent)
  }

  def sourceFileFromPulumiProvider(pulumiPackage: PulumiPackage)(implicit typeMapper: TypeMapper): SourceFile = {
    val asPackageName = pulumiPackage.language.java.packages.withDefault(x => x)

    val providerName = pulumiPackage.name
    val providerPackageSuffix = asPackageName(providerName)
    val pathPrefixParts = basePackage.split("\\.") ++ Seq(providerPackageSuffix, "resources")
    val filePathPrefix = Paths.get(pathPrefixParts.head, pathPrefixParts.tail.toArray: _*)
    sourceFileForResource(
      resourceName = "Provider",
      resourceDefinition = pulumiPackage.provider,
      fullPackageName = s"${basePackage}.${providerPackageSuffix}",
      filePathPrefix = filePathPrefix,
      isProvider = true
    )
  }


  def sourceFilesForNonResourceTypes(pulumiPackage: PulumiPackage)(implicit typeMapper: TypeMapper): Seq[SourceFile] = {
    val asPackageName = pulumiPackage.language.java.packages.withDefault(x => x)
    
    pulumiPackage.types.collect { case (typeToken, typeDefinition) =>
      val typeTokenStruct = typeMapper.parseTypeToken(typeToken)
      val providerName = typeTokenStruct.providerName
      val packageSuffix = typeTokenStruct.packageSuffix
      val typeName = typeTokenStruct.typeName

      val providerPackageSuffix = asPackageName(providerName)

      val pathParts = basePackage.split("\\.") ++ Seq(providerPackageSuffix) ++ packageSuffix.split("\\.") ++ Seq(s"${typeName}.scala")
      val filePath = Paths.get(pathParts.head, pathParts.tail.toArray: _*)



      val packageDecl = s"package ${basePackage}.${providerPackageSuffix}.${packageSuffix}"
      val specificFileContent = typeDefinition match {
        case enumDef: EnumTypeDefinition => sourceForEnum(enumName = typeName, enumDefinition = enumDef)
        case objectDef: ObjectTypeDefinition => sourceForObjectType(typeName = typeName, objectTypeDefinition = objectDef)
      }
      val fileContent = packageDecl + "\n\n" + specificFileContent

      SourceFile(relativePath = filePath, sourceCode = fileContent)
    }.toSeq
  }

  def sourceForEnum(enumName: String, enumDefinition: EnumTypeDefinition): String = {
    val importedIdentifiers = Seq(
      "besom.internal.Decoder"
    )
    val imports = importedIdentifiers.map(id => s"import $id").mkString("\n")

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
      s"""|${imports}
          |
          |enum ${enumName} extends ${superclass} derives Decoder:
          |${enumCases.map(arg => s"  ${arg}").mkString("\n")}
          |""".stripMargin

    fileContent
  }

  def sourceForObjectType(typeName: String, objectTypeDefinition: ObjectTypeDefinition)(implicit typeMapper: TypeMapper): String = {
    val importedIdentifiers = commonImportedIdentifiers ++ Seq(
      "besom.internal.Decoder",
      "besom.internal.Encoder"
    )
    val imports = importedIdentifiers.map(id => s"import $id").mkString("\n")

    val outputName = typeName
    val outputClassName = Type.Name(outputName).syntax
    val argsClassName = Type.Name(s"${outputClassName}Args").syntax

    val outputClassArgs = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
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
      makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsCompanionApplyBodyArgs = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition)
    }

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved
    // val outputComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val outputComment = ""

    val outputClass =
      s"""|case class $outputClassName(
          |${outputClassArgs.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives Decoder""".stripMargin

    val argsClass =
      s"""|case class $argsClassName(
          |${argsClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) derives Encoder""".stripMargin

    val argsCompanion =
      s"""|object $argsClassName:
          |  def apply(
          |${argsCompanionApplyParams.map(param => s"    ${param}").mkString(",\n")}
          |  )(using Context): $argsClassName =
          |    new $argsClassName(
          |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg}").mkString(",\n")}
          |    )""".stripMargin

    val fileContent =
      s"""|${imports}
          |
          |${outputComment}
          |${outputClass}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin

    fileContent
  }

  def sourceFilesForCustomResources(pulumiPackage: PulumiPackage)(implicit typeMapper: TypeMapper): Seq[SourceFile] = {
    val asPackageName = pulumiPackage.language.java.packages.withDefault(x => x)

    pulumiPackage.resources.collect { case (typeToken, resourceDefinition) if !resourceDefinition.isOverlay =>
      
      val typeTokenStruct = typeMapper.parseTypeToken(typeToken)

      val providerName = typeTokenStruct.providerName
      val packageSuffix = typeTokenStruct.packageSuffix
      val resourceName = typeTokenStruct.typeName

      val providerPackageSuffix = asPackageName(providerName)

      val fullPackageName = s"${basePackage}.${providerPackageSuffix}.${packageSuffix}.resources"
      val pathPrefixParts = basePackage.split("\\.") ++ Seq(providerPackageSuffix) ++ packageSuffix.split("\\.") ++ Seq("resources") // TODO
    
      val filePathPrefix = Paths.get(pathPrefixParts.head, pathPrefixParts.tail.toArray: _*)
      
      sourceFileForResource(
        resourceName = resourceName,
        resourceDefinition = resourceDefinition,
        fullPackageName = fullPackageName,
        filePathPrefix = filePathPrefix,
        isProvider = false,
      )
    }.toSeq
  }

  def sourceFileForResource(resourceName: String, resourceDefinition: ResourceDefinition, fullPackageName: String, filePathPrefix: Path, isProvider: Boolean)(implicit typeMapper: TypeMapper): SourceFile = {
    val resourceClassName = Type.Name(resourceName).syntax
    val argsClassName = Type.Name(s"${resourceName}Args").syntax
    val factoryMethodName = Term.Name(decapitalize(resourceName)).syntax

    val conditionallyImportedIdentifiers =
      if (isProvider)
        Seq(
          "besom.internal.ProviderResource",
          "besom.internal.ProviderArgsEncoder"
        )
      else
        Seq(
          "besom.internal.CustomResource",
          "besom.internal.ArgsEncoder"
        )

    val importedIdentifiers = commonImportedIdentifiers ++ conditionallyImportedIdentifiers ++ Seq(
      "besom.internal.ResourceDecoder",
      "besom.internal.CustomResourceOptions"
    )

    val imports = importedIdentifiers.map(id => s"import $id").mkString("\n")

    val resourceBaseProperties = Seq(
      "urn" -> PropertyDefinition(typeReference = StringType),
      "id" -> PropertyDefinition(typeReference = StringType)
    )

    val resourceProperties = resourceBaseProperties ++ resourceDefinition.properties

    val resourceClassParams = resourceProperties.map { case (propertyName, propertyDefinition) =>
      makeResourceClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsClassParams = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsCompanionApplyParams = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsCompanionApplyBodyArgs = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition)
    }

    val packageDecl = s"package ${fullPackageName}"

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved
    // val resourceComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val resourceComment = ""

    val resourceBaseClass = if (isProvider) "ProviderResource" else "CustomResource"

    val resourceClass =
      s"""|case class $resourceClassName(
          |${resourceClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives ResourceDecoder""".stripMargin

    // the type has to match pulumi's resource type schema, ie kubernetes:core/v1:Pod
    val typ = s"" // TODO HIER !!!!!!!!!!!!!!!!!!!!!!!

    val factoryMethod =
      s"""|def $factoryMethodName(using ctx: Context)(
          |  name: String,
          |  args: $argsClassName,
          |  opts: CustomResourceOptions = CustomResourceOptions()
          |): Output[$resourceClassName] = 
          |  ctx.registerResource[$resourceClassName, $argsClassName]($typ, name, args, opts)
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

    val fileContent =
      s"""|$packageDecl
          |
          |${imports}
          |
          |${resourceComment}
          |${resourceClass}
          |
          |${factoryMethod}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin

    val filePath = filePathPrefix.resolve(s"${resourceName}.scala")

    SourceFile(relativePath = filePath, sourceCode = fileContent)
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

  private def makeArgsCompanionApplyParam(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper) = {
    val paramType = property.typeReference match {
      case MapType(additionalProperties) =>
        val valueType = additionalProperties.asScalaType(asArgsType = true)
        t"""Map[String, $valueType] | Map[String, Output[$valueType]] | Output[Map[String, $valueType]] | NotProvided"""
      case tpe =>
        val baseType = tpe.asScalaType(asArgsType = true)
        t"""$baseType | Output[$baseType] | NotProvided"""
    }

    Term.Param(
      mods = List.empty,
      name = Term.Name(propertyName),
      decltpe = Some(paramType),
      default = Some(q"NotProvided")
    ).syntax
  }

  private def makeArgsCompanionApplyBodyArg(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper) = {
    val fieldTermName = Term.Name(propertyName)
    val isSecret = Lit.Boolean(property.secret)
    val argValue = property.typeReference match {
      case MapType(_) =>
        q"${fieldTermName}.asOutputMap(isSecret = ${isSecret})"
      case _ =>
        q"${fieldTermName}.asOutput(isSecret = ${isSecret})"
    }
    Term.Assign(fieldTermName, argValue).syntax
  }

  private def decapitalize(s: String) = s(0).toLower.toString ++ s.substring(1, s.length)
} 

case class SourceFile(
  relativePath: Path,
  sourceCode: String
)


// TODOs:
// * Can providerPackageSuffix be multipart with doty inside?