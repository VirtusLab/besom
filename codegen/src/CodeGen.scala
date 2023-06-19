package besom.codegen

import java.nio.file.{Path, Paths}

import scala.util.matching.Regex
import scala.collection.mutable.ListBuffer

import scala.meta._
import scala.meta.dialects.Scala33

import besom.codegen.metaschema._

object CodeGen {
  // Placeholder module for classes that should be in the root package (according to pulumi's convention)
  val indexModuleName = "index"

  val commonImportedIdentifiers = Seq(
    "besom.util.NotProvided",
    "besom.internal.Output",
    "besom.internal.Context"
  )

  class TypeMapper(
    val moduleToPackageParts: String => Seq[String],
    enumTypeTokens: Set[String],
    objectTypeTokens: Set[String],
    resourceTypeTokens: Set[String],
    moduleFormat: Regex
  ) {
    def parseTypeToken(typeToken: String): PulumiTypeCoordinates = {
      val Array(providerName, modulePortion, typeName) = typeToken.split(":")
      val moduleName = modulePortion match {
        case moduleFormat(name) => name
      }
      PulumiTypeCoordinates(
        providerPackageParts = moduleToPackageParts(providerName),
        modulePackageParts = moduleToPackageParts(moduleName),
        typeName = typeName
      )
    }

    def scalaTypeFromTypeToken(escapedTypeToken: String, isResource: Boolean, asArgsType: Boolean)(implicit logger: Logger) = {
      val typeToken = escapedTypeToken.replace("%2F", "/") // TODO: Proper URL unescaping ?
      val typeCoordinates = parseTypeToken(typeToken)
      val isEnumType = enumTypeTokens.contains(typeToken)
      val classCoordinates = (isResource, isEnumType, asArgsType) match {
        case (true, _, false) => typeCoordinates.asResourceClass
        case (true, _, true) => typeCoordinates.asResourceArgsClass
        case (false, false, false) => typeCoordinates.asObjectClass
        case (false, false, true) => typeCoordinates.asObjectArgsClass
        case (false, true, _) => typeCoordinates.asEnumClass
      }
      classCoordinates.fullyQualifiedTypeRef
    }

    def asScalaType(typeRef: TypeReference, asArgsType: Boolean)(implicit logger: Logger): Type = typeRef match {
      case BooleanType => t"Boolean"
      case StringType => t"String"
      case IntegerType => t"Int"
      case NumberType => t"Double"
      case ArrayType(elemType) => t"scala.collection.immutable.List[${asScalaType(elemType, asArgsType)}]"
      case MapType(elemType) => t"scala.Predef.Map[String, ${asScalaType(elemType, asArgsType)}]"
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
                scalaTypeFromTypeToken(token, isResource = false, asArgsType = asArgsType)
              case s"/resources/${token}" =>
                scalaTypeFromTypeToken(token, isResource = true, asArgsType = asArgsType)
              case s"/${rest}" =>
                throw new Exception(s"Invalid named type reference: ${typeUri}")
              case token =>
                if (objectTypeTokens.contains(token) || enumTypeTokens.contains(token)) {
                  logger.warn(s"Named type reference: ${typeUri} has an invalid format - assuming '#/types/' prefix")
                  scalaTypeFromTypeToken(token, isResource = false, asArgsType = asArgsType)
                } else {
                  logger.warn(s"Named type reference: ${typeUri} has an invalid format and is not defined in the current schema - using underlying primitive type as fallback")
                  asScalaType(namedType.`type`.get, asArgsType = asArgsType)
                }
            }
        }
    }
  }

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper, logger: Logger): Type = typeMapper.asScalaType(typeRef, asArgsType)
  }

  def sourcesFromPulumiPackage(pulumiPackage: PulumiPackage, providerConfig: Config.ProviderConfig, besomVersion: String)(implicit logger: Logger): Seq[SourceFile] = {
    val enumTypeTokensBuffer = ListBuffer.empty[String]
    val objectTypeTokensBuffer = ListBuffer.empty[String]
    
    pulumiPackage.types.foreach {
      case (typeToken, _: EnumTypeDefinition) => enumTypeTokensBuffer += typeToken  
      case (typeToken, _: ObjectTypeDefinition) => objectTypeTokensBuffer += typeToken  
    }

    val enumTypeTokens = enumTypeTokensBuffer.toSet
    val objectTypeTokens = objectTypeTokensBuffer.toSet
    val resourceTypeTokens = pulumiPackage.resources.keySet

    val moduleToPackageParts = pulumiPackage.language.java.packages.view.mapValues { pkg =>
        pkg.split("\\.").filter(_.nonEmpty).toSeq
      }.toMap.withDefault(pkg => Seq(pkg))

    implicit val typeMapper: TypeMapper = new TypeMapper(
      moduleToPackageParts = moduleToPackageParts,
      enumTypeTokens = enumTypeTokens,
      objectTypeTokens = objectTypeTokens,
      resourceTypeTokens = resourceTypeTokens,
      moduleFormat = pulumiPackage.meta.moduleFormat.r
    )

    implicit val providerConf: Config.ProviderConfig = providerConfig

    val scalaSources = (
      sourceFilesForProviderResource(pulumiPackage) ++
      sourceFilesForNonResourceTypes(pulumiPackage) ++
      sourceFilesForCustomResources(pulumiPackage)
    )
    
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
      s"""|//> using scala "3.3.0"
          |//> using lib "org.virtuslab::besom-core:${besomVersion}"
          |
          |//> using resourceDir "resources"
          |
          |//> using publish.organization "org.virtuslab"
          |//> using publish.name "besom-${pulumiPackageName}"
          |//> using publish.version "${besomVersion}"
          |""".stripMargin

    val filePath = FilePath(Seq("project.scala"))

    SourceFile(filePath = filePath, sourceCode = fileContent)
  }

  def resourcePluginMetadataFile(pluginName: String, pluginVersion: String): SourceFile = {
    val fileContent =
      s"""|{
          |  "resource": true,
          |  "name": "${pluginName}",
          |  "version": "${pluginVersion}"
          |}
          |""".stripMargin
    val filePath = FilePath(Seq("resources", "plugin.json"))

    SourceFile(filePath = filePath, sourceCode = fileContent)
  } 

  def sourceFilesForProviderResource(pulumiPackage: PulumiPackage)(implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger): Seq[SourceFile] = {
    val providerName = pulumiPackage.name
    val providerPackageParts = typeMapper.moduleToPackageParts(providerName)
    val typeCoordinates = PulumiTypeCoordinates(
      providerPackageParts = typeMapper.moduleToPackageParts(providerName),
      modulePackageParts = typeMapper.moduleToPackageParts(providerName) :+ indexModuleName,
      typeName = "Provider"
    )
    sourceFilesForResource(
      typeCoordinates = typeCoordinates,
      resourceDefinition = pulumiPackage.provider,
      typeToken = s"pulumi:provider:${pulumiPackage.name}",
      isProvider = true
    )
  }


  def sourceFilesForNonResourceTypes(pulumiPackage: PulumiPackage)(implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger): Seq[SourceFile] = {
    val asPackageName = pulumiPackage.language.java.packages.withDefault(x => x)
    
    pulumiPackage.types.flatMap { case (typeToken, typeDefinition) =>
      val typeCoordinates = typeMapper.parseTypeToken(typeToken)

      typeDefinition match {
        case enumDef: EnumTypeDefinition => sourceFilesForEnum(typeCoordinates = typeCoordinates, enumDefinition = enumDef)
        case objectDef: ObjectTypeDefinition => sourceFilesForObjectType(typeCoordinates = typeCoordinates, objectTypeDefinition = objectDef, typeToken = typeToken)
      }
    }.toSeq
  }

  def sourceFilesForEnum(typeCoordinates: PulumiTypeCoordinates, enumDefinition: EnumTypeDefinition)(implicit providerConfig: Config.ProviderConfig, logger: Logger): Seq[SourceFile] = {
    val classCoordinates = typeCoordinates.asEnumClass

    val enumClassName = Type.Name(classCoordinates.className).syntax
    val enumClassStringName = Lit.String(classCoordinates.className).syntax

    val (superclass, valueType) = enumDefinition.`type` match {
      case BooleanType => ("besom.internal.BooleanEnum", "Boolean")
      case IntegerType => ("besom.internal.IntegerEnum", "Int")
      case NumberType => ("besom.internal.NumberEnum", "Double")
      case StringType => ("besom.internal.StringEnum", "String")
    }

    val instances = enumDefinition.`enum`.map { valueDefinition =>
      val caseRawName = valueDefinition.name.getOrElse {
        valueDefinition.value match {
          case StringConstValue(value) => value
          case const => throw new Exception(s"The name of enum cannot be derived from value ${const}")
        }
      }
      val caseName = Term.Name(caseRawName).syntax
      val caseStringName = Lit.String(caseRawName).syntax
      val caseValue = constValueAsCode(valueDefinition.value).syntax

      val definition = s"""object ${caseName} extends ${enumClassName}(${caseStringName}, ${caseValue})"""
      val reference = caseName
      (definition, reference)
    }

    
    val fileContent =
      s"""|package ${classCoordinates.fullPackageName}
          |
          |sealed abstract class ${enumClassName}(val name: String, val value: ${valueType}) extends ${superclass}
          |
          |object ${enumClassName} extends besom.internal.EnumCompanion[${enumClassName}](${enumClassStringName}):
          |${instances.map(instance => s"  ${instance._1}").mkString("\n")}
          |
          |  override val allInstances: Seq[${enumClassName}] = Seq(
          |${instances.map(instance => s"    ${instance._2}").mkString(",\n")}
          |  )
          |""".stripMargin

    Seq(SourceFile(
      classCoordinates.filePath,
      fileContent
    ))
  }

  def sourceFilesForObjectType(typeCoordinates: PulumiTypeCoordinates, objectTypeDefinition: ObjectTypeDefinition, typeToken: String)(implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger): Seq[SourceFile] = {
    val baseClassCoordinates = typeCoordinates.asObjectClass
    val argsClassCoordinates = typeCoordinates.asObjectArgsClass
    
    val baseClassName = Type.Name(baseClassCoordinates.className).syntax
    val argsClassName = Type.Name(argsClassCoordinates.className).syntax
    
    val baseFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.internal.Decoder"
    ))

    val argsFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.internal.Encoder",
      "besom.internal.ArgsEncoder"
    ))

    val objectProperties = {
      val allProperties = objectTypeDefinition.properties.toSeq.sortBy(_._1)
      if (allProperties.size <= jvmMaxParamsCount)
        allProperties
      else {
        logger.warn(s"Object type ${typeToken} has too many properties. Only first ${jvmMaxParamsCount} will be kept")
        allProperties.take(jvmMaxParamsCount)
      }
    }

    val baseClassParams = objectProperties.map { case (propertyName, propertyDefinition) =>
      val isRequired = objectTypeDefinition.required.contains(propertyName)
      makeNonResourceBaseClassParam(propertyName = propertyName, property = propertyDefinition, isRequired = isRequired)
    }

    val baseOutputExtensionMethods = objectProperties.map { case (propertyName, propertyDefinition) =>
      val isRequired = objectTypeDefinition.required.contains(propertyName)
      makeNonResourceBaseOutputExtensionMethod(propertyName = propertyName, property = propertyDefinition, isRequired = isRequired)
    }

    val argsClassParams = objectProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsCompanionApplyParams = objectProperties.map { case (propertyName, propertyDefinition) =>
      val isRequired = objectTypeDefinition.required.contains(propertyName)
      makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition, isRequired = isRequired)
    }

    val argsCompanionApplyBodyArgs = objectProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition)
    }

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val baseClass =
      s"""|case class $baseClassName(
          |${baseClassParams.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives Decoder""".stripMargin

    val baseCompanion =
      if (hasOutputExtensions) {
        val classNameTerminator = if (baseClassName.endsWith("_")) " " else "" // colon after underscore would be treated as a part of the name

        s"""|object ${baseClassName}${classNameTerminator}:
            |  extension(output: Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"    ${meth}").mkString("\n")}""".stripMargin
      } else {
        s"""object $baseClassName"""
      }
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

    val baseClassFileContent =
      s"""|package ${baseClassCoordinates.fullPackageName}
          |
          |${baseFileImports}
          |
          |${baseClassComment}
          |${baseClass}
          |
          |${baseCompanion}
          |
          |""".stripMargin

    val argsClassFileContent =
      s"""|package ${argsClassCoordinates.fullPackageName} 
          |
          |${argsFileImports}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin

    Seq(
      SourceFile(
        baseClassCoordinates.filePath,
        baseClassFileContent
      ),
      SourceFile(
        argsClassCoordinates.filePath,
        argsClassFileContent
      )
    )
  }

  def sourceFilesForCustomResources(pulumiPackage: PulumiPackage)(implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger): Seq[SourceFile] = {
    pulumiPackage.resources.collect { case (typeToken, resourceDefinition) if !resourceDefinition.isOverlay =>
      sourceFilesForResource(
        typeCoordinates = typeMapper.parseTypeToken(typeToken),
        resourceDefinition = resourceDefinition,
        typeToken = typeToken,
        isProvider = false,
      )
    }.toSeq.flatten
  }

  def sourceFilesForResource(typeCoordinates: PulumiTypeCoordinates, resourceDefinition: ResourceDefinition, typeToken: String, isProvider: Boolean)(implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger): Seq[SourceFile] = {
    val baseClassCoordinates = typeCoordinates.asResourceClass
    val argsClassCoordinates = typeCoordinates.asResourceArgsClass
    val baseClassName = Type.Name(baseClassCoordinates.className).syntax
    val argsClassName = Type.Name(argsClassCoordinates.className).syntax
    val factoryMethodName = Term.Name(decapitalize(baseClassCoordinates.className)).syntax

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

    val resourceProperties = {
      val allProperties = (resourceBaseProperties ++ resourceDefinition.properties.toSeq.sortBy(_._1))
      if (allProperties.size <= jvmMaxParamsCount)
        allProperties
      else {
        logger.warn(s"Resource type ${typeToken} has too many properties. Only first ${jvmMaxParamsCount} will be kept")
        allProperties.take(jvmMaxParamsCount)
      }
    }

    val baseClassParams = resourceProperties.map { case (propertyName, propertyDefinition) =>
      makeResourceBaseClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val baseOutputExtensionMethods = resourceProperties.map { case (propertyName, propertyDefinition) =>
      makeResourceBaseOutputExtensionMethod(propertyName = propertyName, property = propertyDefinition)
    }

    val inputProperties = {
      val allProperties = resourceDefinition.inputProperties.toSeq.sortBy(_._1)
      if (allProperties.size <= jvmMaxParamsCount)
        allProperties
      else {
        logger.warn(s"Resource type ${typeToken} has too many input properties. Only first ${jvmMaxParamsCount} will be kept")
        allProperties.take(jvmMaxParamsCount)
      }
    }

    val argsClassParams = inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsClassParam(propertyName = propertyName, property = propertyDefinition)
    }

    val argsCompanionApplyParams = inputProperties.collect {
      case (propertyName, propertyDefinition) if propertyDefinition.const.isEmpty =>
        val isRequired = resourceDefinition.requiredInputs.contains(propertyName)
        makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition, isRequired)
    }

    val argsCompanionApplyBodyArgs = inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition)
    }

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val resourceBaseClass = if (isProvider) "ProviderResource" else "CustomResource"

    val baseClass =
      s"""|case class $baseClassName(
          |${baseClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives ResourceDecoder""".stripMargin

    val baseCompanion =
      if (hasOutputExtensions) {
        s"""|object $baseClassName:
            |  extension(output: Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"    ${meth}").mkString("\n")}""".stripMargin
      } else {
        s"""object $baseClassName"""
      }

    // the type has to match pulumi's resource type schema, ie kubernetes:core/v1:Pod
    val typ = Lit.String(typeToken)

    val hasDefaultArgsConstructor = resourceDefinition.requiredInputs.forall { propertyName =>
      val propertyDefinition = resourceDefinition.properties(propertyName)
      propertyDefinition.default.nonEmpty || propertyDefinition.const.nonEmpty
    }
    val argsDefault = if (hasDefaultArgsConstructor) s""" = ${argsClassName}()""" else ""
    val factoryMethod =
      s"""|def $factoryMethodName(using ctx: Context)(
          |  name: NonEmptyString,
          |  args: ${argsClassName}${argsDefault},
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
      s"""|package ${baseClassCoordinates.fullPackageName}
          |
          |${baseFileImports}
          |
          |${baseClassComment}
          |${baseClass}
          |
          |${baseCompanion}
          |
          |${factoryMethod}
          |""".stripMargin

    val argsClassFileContent =
      s"""|package ${argsClassCoordinates.fullPackageName}
          |
          |${argsFileImports}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin

    Seq(
      SourceFile(
        baseClassCoordinates.filePath,
        baseClassFileContent
      ),
      SourceFile(
        argsClassCoordinates.filePath,
        argsClassFileContent
      )
    )
  }

  private def makeResourceBaseClassParam(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper, logger: Logger) = {
    val fieldBaseType = property.typeReference.asScalaType()
    val fieldType = t"Output[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(manglePropertyName(propertyName)),
      decltpe = Some(fieldType),
      default = None
    ).syntax
  }

  private def makeNonResourceBaseClassParam(propertyName: String, property: PropertyDefinition, isRequired: Boolean)(implicit typeMapper: TypeMapper, logger: Logger) = {
    val fieldBaseType = property.typeReference.asScalaType()
    val fieldType = if (isRequired) fieldBaseType else t"Option[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(manglePropertyName(propertyName)),
      decltpe = Some(fieldType),
      default = None
    ).syntax
  }

  private def makeResourceBaseOutputExtensionMethod(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper, logger: Logger) = {
    val propertyTermName = Term.Name(manglePropertyName(propertyName))
    val fieldBaseType = property.typeReference.asScalaType()
    val fieldType = t"Output[$fieldBaseType]"
    q"""def ${propertyTermName}: $fieldType = output.flatMap(_.${propertyTermName})""".syntax
  }

  private def makeNonResourceBaseOutputExtensionMethod(propertyName: String, property: PropertyDefinition, isRequired: Boolean)(implicit typeMapper: TypeMapper, logger: Logger) = {
    val propertyTermName = Term.Name(manglePropertyName(propertyName))
    val fieldBaseType = property.typeReference.asScalaType()
    val fieldType = if (isRequired) t"Output[$fieldBaseType]" else t"Output[Option[$fieldBaseType]]"
    q"""def ${propertyTermName}: $fieldType = output.map(_.${propertyTermName})""".syntax
  }

  private def makeArgsClassParam(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper, logger: Logger) = {
    val fieldBaseType = property.typeReference.asScalaType(asArgsType = true)
    val fieldType = t"Output[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(manglePropertyName(propertyName)),
      decltpe = Some(fieldType),
      default = None
    ).syntax
  }

  private def makeArgsCompanionApplyParam(propertyName: String, property: PropertyDefinition, isRequired: Boolean)(implicit typeMapper: TypeMapper, logger: Logger) = {
    val requiredParamType = property.typeReference match {
      case MapType(additionalProperties) =>
        val valueType = additionalProperties.asScalaType(asArgsType = true)
        t"""scala.Predef.Map[String, $valueType] | scala.Predef.Map[String, Output[$valueType]] | Output[scala.Predef.Map[String, $valueType]]"""
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
      name = Term.Name(manglePropertyName(propertyName)),
      decltpe = Some(paramType),
      default = default
    ).syntax
  }

  private def makeArgsCompanionApplyBodyArg(propertyName: String, property: PropertyDefinition)(implicit typeMapper: TypeMapper, logger: Logger) = {
    val fieldTermName = Term.Name(manglePropertyName(propertyName))
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

  private val jvmMaxParamsCount = 253 // https://github.com/scala/bug/issues/7324 // TODO: Find some workaround to enable passing the remaining arguments

  private def decapitalize(s: String) = s(0).toLower.toString ++ s.substring(1, s.length)

  private val anyRefMethodNames = Set(
    "eq",
    "ne",
    "notify",
    "notifyAll",
    "synchronized",
    "wait",
    "asInstanceOf",
    "equals",
    "getClass",
    "hashCode",
    "isInstanceOf",
    "toString",
  )

  // This logic must be undone the same way in codecs
  // Keep in sync with `unmanglePropertyName` in codecs.scala
  private def manglePropertyName(name: String)(implicit logger: Logger): String = 
    if (anyRefMethodNames.contains(name)) {
      val mangledName = name + "_"
      logger.warn(s"Mangled property name '$name' as '$mangledName'")
      mangledName
    } else name

  private def makeImportStatements(importedIdentifiers: Seq[String]) =
    importedIdentifiers.map(id => s"import $id").mkString("\n")
}

object PulumiTypeCoordinates {
  private def capitalize(s: String) = s(0).toUpper.toString ++ s.substring(1, s.length)
  private val maxNameLength = 200

  // This naïvely tries to avoid the limitation on the length of file paths in a file system
  // TODO: truncated file names with a suffix might still potentially clash with each other
  private def uniquelyTruncateTypeName(name: String)(implicit logger: Logger) =
    if (name.length <= maxNameLength) {
      name
    } else {
      val preservedPrefix = name.substring(0, maxNameLength)
      val removedSuffixHash = Math.abs(name.substring(maxNameLength, name.length).hashCode)
      val truncatedName = s"${preservedPrefix}__${removedSuffixHash}__"

      truncatedName
    }

  private def mangleTypeName(name: String)(implicit logger: Logger) = {
    val mangledName = capitalize(uniquelyTruncateTypeName(name))
    if (mangledName != name)
      logger.warn(s"Mangled type name '$name' as '$mangledName'")
    mangledName
  }
}

case class PulumiTypeCoordinates(
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  private val typeName: String,
) {
  import PulumiTypeCoordinates._

  def asResourceClass(implicit logger: Logger) = ClassCoordinates(
    providerPackageParts = providerPackageParts,
    modulePackageParts = modulePackageParts,
    className = mangleTypeName(typeName)
  )
  def asResourceArgsClass(implicit logger: Logger) = ClassCoordinates(
    providerPackageParts = providerPackageParts,
    modulePackageParts = modulePackageParts,
    className = mangleTypeName(typeName) ++ "Args"
  )
  def asObjectClass(implicit logger: Logger) = ClassCoordinates(
    providerPackageParts = providerPackageParts,
    modulePackageParts = modulePackageParts :+ "outputs",
    className = mangleTypeName(typeName)
  )
  def asObjectArgsClass(implicit logger: Logger) = ClassCoordinates(
    providerPackageParts = providerPackageParts,
    modulePackageParts = modulePackageParts :+ "inputs",
    className = mangleTypeName(typeName) ++ "Args"
  )
  def asEnumClass(implicit logger: Logger) = ClassCoordinates(
    providerPackageParts = providerPackageParts,
    modulePackageParts = modulePackageParts :+ "enums",
    className = mangleTypeName(typeName)
  )
}

object ClassCoordinates {
  val baseApiPackagePrefixParts = Seq("besom", "api")
}

case class ClassCoordinates(
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  val className: String,
) {
  import ClassCoordinates._

  private def packageRef: Term.Ref = {
    val moduleParts = if (modulePackageParts.head == CodeGen.indexModuleName) modulePackageParts.tail else modulePackageParts
    val partsHead :: partsTail = baseApiPackagePrefixParts ++ providerPackageParts ++ moduleParts
    partsTail.foldLeft[Term.Ref](Term.Name(partsHead))((acc, name) => Term.Select(acc, Term.Name(name)))
  }

  def fullyQualifiedTypeRef: Type.Ref = {
    Type.Select(packageRef, Type.Name(className))
  }

  def fullPackageName: String = {
    packageRef.syntax
  }

  def filePath(implicit providerConfig: Config.ProviderConfig): FilePath = {
    val moduleName = modulePackageParts.head
    val moduleParts =
      if (providerConfig.noncompiledModules.contains(moduleName))
        s".${moduleName}" +: modulePackageParts.tail // A leading dot excludes a directory from scala-cli sources
      else
        modulePackageParts
    FilePath(Seq("src") ++ moduleParts ++ Seq(s"${className}.scala"))
  }
}

case class FilePath(pathParts: Seq[String]) {
  def osSubPath: os.SubPath = pathParts.foldLeft(os.sub)(_ / _)
}

case class SourceFile(
  filePath: FilePath,
  sourceCode: String
)
