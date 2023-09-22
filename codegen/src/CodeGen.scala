package besom.codegen

import java.nio.file.{Path, Paths}

import scala.meta._
import scala.meta.dialects.Scala33

import besom.codegen.metaschema._
import besom.codegen.Utils._

class CodeGen(implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger) {
  val commonImportedIdentifiers = Seq(
    "besom.types.Output",
    "besom.types.Context"
  )

  def sourcesFromPulumiPackage(pulumiPackage: PulumiPackage, schemaVersion: String, besomVersion: String): Seq[SourceFile] = {
    val scalaSources = (
      sourceFilesForProviderResource(pulumiPackage) ++
      sourceFilesForNonResourceTypes(pulumiPackage) ++
      sourceFilesForCustomResources(pulumiPackage)
    )

    scalaSources ++ Seq(
      projectConfigFile(
        pulumiPackageName = pulumiPackage.name,
        schemaVersion = schemaVersion,
        besomVersion = besomVersion
      ),
      resourcePluginMetadataFile(
        pluginName = pulumiPackage.name,
        pluginVersion = schemaVersion,
        pluginDownloadUrl = pulumiPackage.pluginDownloadURL
      ),
    )
  }

  def projectConfigFile(pulumiPackageName: String, schemaVersion: String, besomVersion: String): SourceFile = {
    // TODO use original package version from the schema as publish.version?

    val fileContent =
      s"""|//> using scala "3.3.0"
          |//> using options "-java-output-version:11"
          |//> using options "-skip-by-regex:.*"
          |
          |//> using dep "org.virtuslab::besom-core:${besomVersion}"
          |
          |//> using resourceDir "resources"
          |
          |//> using publish.name "besom-${pulumiPackageName}"
          |//> using publish.organization "org.virtuslab"
          |//> using publish.version "${schemaVersion}-core.${besomVersion}"
          |//> using publish.url "https://github.com/VirtusLab/besom"
          |//> using publish.vcs "github:VirtusLab/besom"
          |//> using publish.license "Apache-2.0"
          |//> using publish.repository "central"
          |//> using publish.developer "lbialy|Łukasz Biały|https://github.com/lbialy"
          |//> using publish.developer "prolativ|Michał Pałka|https://github.com/prolativ"
          |//> using publish.developer "KacperFKorban|Kacper Korban|https://github.com/KacperFKorban"
          |""".stripMargin

    val filePath = FilePath(Seq("project.scala"))

    SourceFile(filePath = filePath, sourceCode = fileContent)
  }

  def resourcePluginMetadataFile(pluginName: String, pluginVersion: String, pluginDownloadUrl: Option[String]): SourceFile = {
    val pluginDownloadUrlJsonValue = pluginDownloadUrl match {
      case Some(url) => s"\"${url}\""
      case None => "null"
    }
    val fileContent =
      s"""|{
          |  "resource": true,
          |  "name": "${pluginName}",
          |  "version": "${pluginVersion}",
          |  "server": ${pluginDownloadUrlJsonValue}
          |}
          |""".stripMargin
    val filePath = FilePath(Seq("resources", "besom", "api", pluginName, "plugin.json"))

    SourceFile(filePath = filePath, sourceCode = fileContent)
  } 

  def sourceFilesForProviderResource(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    val providerName = pulumiPackage.name
    val providerPackageParts = typeMapper.defaultPackageInfo.moduleToPackageParts(providerName)
    val typeCoordinates = PulumiTypeCoordinates(
      providerPackageParts = typeMapper.defaultPackageInfo.moduleToPackageParts(providerName),
      modulePackageParts = Seq(Utils.indexModuleName),
      typeName = "Provider"
    )
    sourceFilesForResource(
      typeCoordinates = typeCoordinates,
      resourceDefinition = pulumiPackage.provider,
      typeToken = s"pulumi:provider:${pulumiPackage.name}",
      isProvider = true
    )
  }


  def sourceFilesForNonResourceTypes(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    val moduleToPackageParts = pulumiPackage.moduleToPackageParts
    
    pulumiPackage.types.flatMap { case (typeToken, typeDefinition) =>
      val typeCoordinates = typeMapper.parseTypeToken(typeToken, moduleToPackageParts)

      typeDefinition match {
        case enumDef: EnumTypeDefinition => sourceFilesForEnum(typeCoordinates = typeCoordinates, enumDefinition = enumDef)
        case objectDef: ObjectTypeDefinition => sourceFilesForObjectType(typeCoordinates = typeCoordinates, objectTypeDefinition = objectDef, typeToken = typeToken)
      }
    }.toSeq
  }

  def sourceFilesForEnum(typeCoordinates: PulumiTypeCoordinates, enumDefinition: EnumTypeDefinition): Seq[SourceFile] = {
    val classCoordinates = typeCoordinates.asEnumClass

    val enumClassName = Type.Name(classCoordinates.className).syntax
    val enumClassStringName = Lit.String(classCoordinates.className).syntax

    val (superclass, valueType) = enumDefinition.`type` match {
      case BooleanType => ("besom.types.BooleanEnum", "Boolean")
      case IntegerType => ("besom.types.IntegerEnum", "Int")
      case NumberType => ("besom.types.NumberEnum", "Double")
      case StringType => ("besom.types.StringEnum", "String")
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
          |object ${enumClassName} extends besom.types.EnumCompanion[${enumClassName}](${enumClassStringName}):
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

  def sourceFilesForObjectType(typeCoordinates: PulumiTypeCoordinates, objectTypeDefinition: ObjectTypeDefinition, typeToken: String): Seq[SourceFile] = {
    val baseClassCoordinates = typeCoordinates.asObjectClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asObjectClass(asArgsType = true)
    
    val baseClassName = Type.Name(baseClassCoordinates.className).syntax
    val argsClassName = Type.Name(argsClassCoordinates.className).syntax
    
    val baseFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.types.Decoder"
    ))

    val argsFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.types.Input",
      "besom.types.Encoder",
      "besom.types.ArgsEncoder"
    ))

    val objectProperties = {
      val allProperties = objectTypeDefinition.properties.toSeq.sortBy(_._1)
      val truncatedProperties =
        if (allProperties.size <= jvmMaxParamsCount)
          allProperties
        else {
          logger.warn(s"Object type ${typeToken} has too many properties. Only first ${jvmMaxParamsCount} will be kept")
          allProperties.take(jvmMaxParamsCount)
        }
      
      truncatedProperties.map {
        case (propertyName, propertyDefinition) =>
          val isPropertyRequired = objectTypeDefinition.required.contains(propertyName)
          makePropertyInfo(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = objectTypeDefinition.required.contains(propertyName)
          )
      }
    }

    val baseClassParams = objectProperties.map { propertyInfo =>
      val fieldType =  if (propertyInfo.isOptional) t"""Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      Term.Param(
        mods = List.empty,
          name = propertyInfo.name,
          decltpe = Some(fieldType),
          default = None
        ).syntax
      }
    val baseOutputExtensionMethods = objectProperties.map { propertyInfo =>
      val innerType = if (propertyInfo.isOptional) t"""Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      q"""def ${propertyInfo.name}: Output[$innerType] = output.map(_.${propertyInfo.name})""".syntax
    }
    val baseOptionOutputExtensionMethods = objectProperties.map { propertyInfo =>
      val innerMethodName = if (propertyInfo.isOptional) Term.Name("flatMap") else Term.Name("map")
      q"""def ${propertyInfo.name}: Output[Option[${propertyInfo.baseType}]] = output.map(_.${innerMethodName}(_.${propertyInfo.name}))""".syntax
    }

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val baseClass =
      s"""|final case class $baseClassName private(
          |${baseClassParams.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives Decoder""".stripMargin

    val baseCompanion =
      if (hasOutputExtensions) {
        val classNameTerminator = if (baseClassName.endsWith("_")) " " else "" // colon after underscore would be treated as a part of the name

        s"""|object ${baseClassName}${classNameTerminator}:
            |  given outputOps: {} with
            |    extension(output: Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}
            |
            |  given optionOutputOps: {} with
            |    extension(output: Output[Option[$baseClassName]])
            |${baseOptionOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}
            |""".stripMargin
      } else {
        s"""object $baseClassName"""
      }
    
    val argsClass = makeArgsClass(argsClassName = argsClassName, inputProperties = objectProperties, isResource = false, isProvider = false)
    val argsCompanion = makeArgsCompanion(argsClassName = argsClassName, inputProperties = objectProperties, isResource = false)

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

  def sourceFilesForCustomResources(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    val moduleToPackageParts = pulumiPackage.moduleToPackageParts

    pulumiPackage.resources.collect { case (typeToken, resourceDefinition) if !resourceDefinition.isOverlay =>
      sourceFilesForResource(
        typeCoordinates = typeMapper.parseTypeToken(typeToken, moduleToPackageParts),
        resourceDefinition = resourceDefinition,
        typeToken = typeToken,
        isProvider = false,
      )
    }.toSeq.flatten
  }

  def sourceFilesForResource(typeCoordinates: PulumiTypeCoordinates, resourceDefinition: ResourceDefinition, typeToken: String, isProvider: Boolean): Seq[SourceFile] = {
    val baseClassCoordinates = typeCoordinates.asResourceClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asResourceClass(asArgsType = true)
    val baseClassName = Type.Name(baseClassCoordinates.className).syntax
    val argsClassName = Type.Name(argsClassCoordinates.className).syntax

    val baseFileImports = {
      val conditionalIdentifiers =
        if (isProvider)
          Seq("besom.ProviderResource")
        else
          Seq("besom.CustomResource")
      val unconditionalIdentifiers = Seq(
        "besom.ResourceDecoder",
        "besom.CustomResourceOptions",
        "besom.util.NonEmptyString",
      )
      makeImportStatements(commonImportedIdentifiers ++ conditionalIdentifiers ++ unconditionalIdentifiers)
    }

    val argsFileImports = {
      val baseIdentifiers = Seq(
        "besom.Input",
        "besom.Encoder"
      )
      val conditionalIdentifiers =
        if (isProvider)
          Seq("besom.ProviderArgsEncoder")
        else
          Seq("besom.ArgsEncoder")
      makeImportStatements(commonImportedIdentifiers ++ baseIdentifiers ++ conditionalIdentifiers)
    }

    val resourceBaseProperties = Seq(
      "urn" -> PropertyDefinition(typeReference = UrnType),
      "id" -> PropertyDefinition(typeReference = ResourceIdType)
    )

    val requiredOutputs = (resourceDefinition.required ++ List("urn", "id")).toSet

    val resourceProperties = {
      val allProperties = (resourceBaseProperties ++ resourceDefinition.properties.toSeq.sortBy(_._1))
      val truncatedProperties =
        if (allProperties.size <= jvmMaxParamsCount)
          allProperties
        else {
          logger.warn(s"Resource type ${typeToken} has too many properties. Only first ${jvmMaxParamsCount} will be kept")
          allProperties.take(jvmMaxParamsCount)
        }
      truncatedProperties.map {
        case (propertyName, propertyDefinition) =>
          makePropertyInfo(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = requiredOutputs.contains(propertyName)
          )
      }
    }

    val requiredInputs = resourceDefinition.requiredInputs.toSet

    val baseClassParams = resourceProperties.map { propertyInfo =>
      val innerType = if (propertyInfo.isOptional) t"""Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      Term.Param(
      mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(t"""Output[${innerType}]"""),
        default = None
      ).syntax
    }

    val baseOutputExtensionMethods = resourceProperties.map { propertyInfo =>
      val innerType = if (propertyInfo.isOptional) t"""Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      val resultType = t"Output[${innerType}]"
      q"""def ${propertyInfo.name}: $resultType = output.flatMap(_.${propertyInfo.name})""".syntax
    }

    val inputProperties = {
      val allProperties = resourceDefinition.inputProperties.toSeq.sortBy(_._1)
      val truncatedProperties = 
        if (allProperties.size <= jvmMaxParamsCount)
          allProperties
        else {
          logger.warn(s"Resource type ${typeToken} has too many input properties. Only first ${jvmMaxParamsCount} will be kept")
          allProperties.take(jvmMaxParamsCount)
        }
      truncatedProperties.map {
        case (propertyName, propertyDefinition) =>
          makePropertyInfo(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = requiredInputs.contains(propertyName)
          )
      }
    }

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val resourceBaseClass = if (isProvider) "ProviderResource" else "CustomResource"

    val baseClass =
      s"""|final case class $baseClassName private(
          |${baseClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives ResourceDecoder""".stripMargin

    val hasDefaultArgsConstructor = resourceDefinition.requiredInputs.forall { propertyName =>
      val propertyDefinition = resourceDefinition.inputProperties(propertyName)
      propertyDefinition.default.nonEmpty || propertyDefinition.const.nonEmpty
    }

    val argsDefault = if (hasDefaultArgsConstructor) s""" = ${argsClassName}()""" else ""

    // the type has to match pulumi's resource type schema, ie kubernetes:core/v1:Pod
    val typ = Lit.String(typeToken)

    val baseCompanion =
      if (hasOutputExtensions) {
        s"""|object $baseClassName:
            |  def apply(using ctx: Context)(
            |    name: NonEmptyString,
            |    args: ${argsClassName}${argsDefault},
            |    opts: CustomResourceOptions = CustomResourceOptions()
            |  ): Output[$baseClassName] = 
            |    ctx.registerResource[$baseClassName, $argsClassName](${typ}, name, args, opts)
            |
            |  given outputOps: {} with
            |    extension(output: Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}""".stripMargin
      } else {
        s"""object $baseClassName"""
      }

    val argsClass = makeArgsClass(argsClassName = argsClassName, inputProperties = inputProperties, isResource = true, isProvider = isProvider)

    val argsCompanion = makeArgsCompanion(argsClassName = argsClassName, inputProperties = inputProperties, isResource = true)

    val baseClassFileContent =
      s"""|package ${baseClassCoordinates.fullPackageName}
          |
          |${baseFileImports}
          |
          |${baseClassComment}
          |${baseClass}
          |
          |${baseCompanion}
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

  private def makePropertyInfo(propertyName: String, propertyDefinition: PropertyDefinition, isPropertyRequired: Boolean): PropertyInfo = {
    val isRequired =
        isPropertyRequired ||
        propertyDefinition.default.nonEmpty ||
        propertyDefinition.const.nonEmpty
    val baseType = propertyDefinition.typeReference.asScalaType()
    val argType = propertyDefinition.typeReference.asScalaType(asArgsType = true)
    val inputArgType = propertyDefinition.typeReference match {
      case ArrayType(innerType) =>
        t"""List[Input[${innerType.asScalaType(asArgsType = true)}]]"""
      case MapType(innerType) =>
        t"""scala.Predef.Map[String, Input[${innerType.asScalaType(asArgsType = true)}]]"""
      case tp =>
        tp.asScalaType(asArgsType = true)
    }
    val defaultValue =
      propertyDefinition.default.orElse(propertyDefinition.const)
        .map { value =>
          constValueAsCode(value)
        }.orElse {
          if (isPropertyRequired) None else Some(q"""None""")
        }
    val constValue = propertyDefinition.const.map(constValueAsCode)

    PropertyInfo(
      name = Term.Name(manglePropertyName(propertyName)),
      isOptional = !isRequired,
      baseType = baseType,
      argType = argType,
      inputArgType = inputArgType,
      defaultValue = defaultValue,
      constValue = constValue,
      isSecret = propertyDefinition.secret,
    )
  }

  private def makeArgsClass(argsClassName: String, inputProperties: Seq[PropertyInfo], isResource: Boolean, isProvider: Boolean) = {
    val derivedTypeclasses =
      if (isProvider) "ProviderArgsEncoder"
      else if (isResource)  "ArgsEncoder"
      else "Encoder, ArgsEncoder"
    val argsClassParams = inputProperties.map { propertyInfo =>
      val fieldType = if (propertyInfo.isOptional) t"""Option[${propertyInfo.argType}]""" else propertyInfo.argType
      Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(t"""Output[${fieldType}]"""),
        default = None
      ).syntax
    }

    s"""|final case class $argsClassName private(
        |${argsClassParams.map(arg => s"  ${arg}").mkString(",\n")}
                                          |) derives ${derivedTypeclasses}""".stripMargin
  }

  private def makeArgsCompanion(argsClassName: String, inputProperties: Seq[PropertyInfo], isResource: Boolean) = {
    val argsCompanionApplyParams = inputProperties.filter(_.constValue.isEmpty).map { propertyInfo =>
      val paramType = if (propertyInfo.isOptional) t"""Input.Optional[${propertyInfo.inputArgType}]""" else t"""Input[${propertyInfo.inputArgType}]"""
      Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(paramType),
        default = propertyInfo.defaultValue,
      ).syntax
    }
    
    val argsCompanionApplyBodyArgs = inputProperties.map { propertyInfo =>
      val isSecret = Lit.Boolean(propertyInfo.isSecret)
      val argValue = propertyInfo.constValue match {
        case Some(constValue) =>
          q"Output(${constValue})"
        case None =>
          if (propertyInfo.isOptional)
            q"${propertyInfo.name}.asOptionOutput(isSecret = ${isSecret})"
          else
            q"${propertyInfo.name}.asOutput(isSecret = ${isSecret})"
      }
      Term.Assign(propertyInfo.name, argValue).syntax
    }

    s"""|object $argsClassName:
        |  def apply(
        |${argsCompanionApplyParams.map(arg => s"    ${arg}").mkString(",\n")}
        |  )(using Context): $argsClassName =
        |    new $argsClassName(
        |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg}").mkString(",\n")}
        |    )""".stripMargin
  }

  private def constValueAsCode(constValue: ConstValue) = constValue match {
    case StringConstValue(value) =>
      Lit.String(value)
    case BooleanConstValue(value) =>
      Lit.Boolean(value)
    case IntConstValue(value) =>
      Lit.Int(value)
    case DoubleConstValue(value) =>
      Lit.Double(value)
  }

  private def decapitalize(s: String) = s(0).toLower.toString ++ s.substring(1, s.length)

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

case class FilePath(pathParts: Seq[String]) {
  def osSubPath: os.SubPath = pathParts.foldLeft(os.sub)(_ / _)
}

case class SourceFile(
  filePath: FilePath,
  sourceCode: String
)
