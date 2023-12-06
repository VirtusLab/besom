package besom.codegen

import besom.codegen.Config.{CodegenConfig, ProviderConfig}
import besom.codegen.PackageVersion
import besom.codegen.Utils.*
import besom.codegen.metaschema.*
import besom.codegen.scalameta.interpolator.*

import scala.meta.*
import scala.meta.dialects.Scala33

//noinspection ScalaWeakerAccess,TypeAnnotation
class CodeGen(implicit
  codegenConfig: CodegenConfig,
  providerConfig: ProviderConfig,
  typeMapper: TypeMapper,
  schemaProvider: SchemaProvider,
  logger: Logger
) {

  def sourcesFromPulumiPackage(
    pulumiPackage: PulumiPackage,
    packageInfo: PulumiPackageInfo
  ): Seq[SourceFile] =
    scalaFiles(pulumiPackage) ++ Seq(
      projectConfigFile(
        schemaName = packageInfo.name,
        packageVersion = packageInfo.version
      ),
      resourcePluginMetadataFile(
        pluginName = packageInfo.name,
        pluginVersion = packageInfo.version,
        pluginDownloadUrl = pulumiPackage.pluginDownloadURL
      )
    )

  def scalaFiles(pulumiPackage: PulumiPackage): Seq[SourceFile] =
    sourceFilesForProviderResource(pulumiPackage) ++
      sourceFilesForNonResourceTypes(pulumiPackage) ++
      sourceFilesForResources(pulumiPackage) ++
      sourceFilesForFunctions(pulumiPackage) ++
      sourceFilesForConfig(pulumiPackage)

  def projectConfigFile(schemaName: String, packageVersion: PackageVersion): SourceFile = {
    val besomVersion = codegenConfig.besomVersion
    val scalaVersion = codegenConfig.scalaVersion
    val javaVersion  = codegenConfig.javaVersion

    val dependencies = schemaProvider
      .dependencies(schemaName, packageVersion)
      .map { case (name, version) =>
        s"""|//> using dep "org.virtuslab::besom-${name}:${version}-core.${besomVersion}"
            |""".stripMargin
      }
      .mkString("\n")

    val fileContent =
      s"""|//> using scala "$scalaVersion"
          |//> using options "-java-output-version:$javaVersion"
          |//> using options "-skip-by-regex:.*"
          |
          |//> using dep "org.virtuslab::besom-core:${besomVersion}"
          |${dependencies}
          |//> using resourceDir "resources"
          |
          |//> using publish.name "besom-${schemaName}"
          |//> using publish.organization "org.virtuslab"
          |//> using publish.version "${packageVersion}-core.${besomVersion}"
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

  def resourcePluginMetadataFile(
    pluginName: String,
    pluginVersion: PackageVersion,
    pluginDownloadUrl: Option[String]
  ): SourceFile = {
    val pluginDownloadUrlJsonValue = pluginDownloadUrl match {
      case Some(url) => s"\"${url}\""
      case None      => "null"
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
    val typeToken              = pulumiPackage.providerTypeToken
    val moduleToPackageParts   = pulumiPackage.moduleToPackageParts
    val providerToPackageParts = pulumiPackage.providerToPackageParts
    val typeCoordinates =
      PulumiDefinitionCoordinates.fromRawToken(typeToken, moduleToPackageParts, providerToPackageParts)

    sourceFilesForResource(
      typeCoordinates = typeCoordinates,
      resourceDefinition = pulumiPackage.provider,
      methods = pulumiPackage.parsedMethods(pulumiPackage.provider),
      isProvider = true
    )
  }

  def sourceFilesForNonResourceTypes(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    pulumiPackage.parsedTypes.flatMap { case (coordinates, typeDefinition) =>
      typeDefinition match {
        case enumDef: EnumTypeDefinition =>
          sourceFilesForEnum(typeCoordinates = coordinates, enumDefinition = enumDef)
        case objectDef: ObjectTypeDefinition =>
          sourceFilesForObjectType(typeCoordinates = coordinates, objectTypeDefinition = objectDef)
      }
    }.toSeq
  }

  def sourceFilesForEnum(
    typeCoordinates: PulumiDefinitionCoordinates,
    enumDefinition: EnumTypeDefinition
  ): Seq[SourceFile] = {
    val classCoordinates = typeCoordinates.asEnumClass

    val enumClassName = classCoordinates.definitionTermName.getOrElse(
      throw GeneralCodegenException(
        s"Class name for ${classCoordinates.typeRef} could not be found"
      )
    )
    val enumClassStringName = classCoordinates.definitionName
      .map(Lit.String(_))
      .getOrElse(
        throw GeneralCodegenException(
          s"Class name for ${classCoordinates.typeRef} could not be found"
        )
      )

    val (superclass, valueType) = enumDefinition.`type` match {
      case BooleanType => (scalameta.types.besom.types.BooleanEnum, scalameta.types.Boolean)
      case IntegerType => (scalameta.types.besom.types.IntegerEnum, scalameta.types.Int)
      case NumberType  => (scalameta.types.besom.types.NumberEnum, scalameta.types.Double)
      case StringType  => (scalameta.types.besom.types.StringEnum, scalameta.types.String)
    }

    val instances: Seq[(Stat, Term.Name)] = enumDefinition.`enum`.map { (valueDefinition: EnumValueDefinition) =>
      val caseRawName = valueDefinition.name.getOrElse {
        valueDefinition.value match {
          case StringConstValue(value) => value
          case const                   => throw GeneralCodegenException(s"The name of enum cannot be derived from value ${const}")
        }
      }
      val caseName       = Term.Name(caseRawName)
      val caseStringName = Lit.String(caseRawName)
      val caseValue      = valueDefinition.value.asScala

      val definition = m"""object ${caseName} extends ${enumClassName}(${caseStringName}, ${caseValue})""".parse[Stat].get
      val reference  = caseName
      (definition, reference)
    }

    val fileContent =
      m"""|package ${classCoordinates.packageRef}
          |
          |sealed abstract class ${enumClassName}(val name: String, val value: ${valueType}) extends ${superclass}
          |
          |object ${enumClassName} extends besom.types.EnumCompanion[${valueType}, ${enumClassName}](${enumClassStringName}):
          |${instances.map(instance => s"  ${instance._1.syntax}").mkString("\n")}
          |
          |  override val allInstances: Seq[${enumClassName}] = Seq(
          |${instances.map(instance => s"    ${instance._2.syntax}").mkString(",\n")}
          |  )
          |""".stripMargin.parse[Source].get

    Seq(
      SourceFile(
        classCoordinates.filePath,
        fileContent.syntax
      )
    )
  }

  def sourceFilesForObjectType(
    typeCoordinates: PulumiDefinitionCoordinates,
    objectTypeDefinition: ObjectTypeDefinition
  ): Seq[SourceFile] = {
    val typeToken            = typeCoordinates.token
    val baseClassCoordinates = typeCoordinates.asObjectClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asObjectClass(asArgsType = true)

    val objectProperties = {
      val allProperties = objectTypeDefinition.properties.toSeq.sortBy(_._1)
      val truncatedProperties =
        if (allProperties.size <= jvmMaxParamsCount) allProperties
        else {
          logger.warn(
            s"Object type ${typeToken.asString} has too many properties. Only first ${jvmMaxParamsCount} will be kept"
          )
          allProperties.take(jvmMaxParamsCount)
        }

      truncatedProperties.map { case (propertyName, propertyDefinition) =>
        PropertyInfo.from(
          propertyName = propertyName,
          propertyDefinition = propertyDefinition,
          isPropertyRequired = objectTypeDefinition.required.contains(propertyName)
        )
      }
    }

    val baseClassSourceFile = makeOutputClassSourceFile(baseClassCoordinates, objectProperties)

    val argsClassSourceFile = makeArgsClassSourceFile(
      classCoordinates = argsClassCoordinates,
      properties = objectProperties,
      isResource = false,
      isProvider = false
    )

    Seq(baseClassSourceFile, argsClassSourceFile)
  }

  def sourceFilesForResources(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    pulumiPackage.parsedResources
      .map { case (coordinates, resourceDefinition) =>
        sourceFilesForResource(
          typeCoordinates = coordinates,
          resourceDefinition = resourceDefinition,
          methods = pulumiPackage.parsedMethods(resourceDefinition),
          isProvider = false
        )
      }
      .toSeq
      .flatten
  }

  def sourceFilesForResource(
    typeCoordinates: PulumiDefinitionCoordinates,
    resourceDefinition: ResourceDefinition,
    methods: Map[FunctionName, (PulumiDefinitionCoordinates, FunctionDefinition)],
    isProvider: Boolean
  ): Seq[SourceFile] = {
    val token                = typeCoordinates.token
    val baseClassCoordinates = typeCoordinates.asResourceClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asResourceClass(asArgsType = true)
    val baseClassName = baseClassCoordinates.definitionTypeName.getOrElse(
      throw GeneralCodegenException(
        s"Class name for ${baseClassCoordinates.typeRef} could not be found"
      )
    )
    val argsClassName = argsClassCoordinates.definitionTermName.getOrElse(
      throw GeneralCodegenException(
        s"Class name for ${argsClassCoordinates.typeRef} could not be found"
      )
    )

    if (resourceDefinition.aliases.nonEmpty) {
      logger.warn(
        s"Aliases are not supported yet, ignoring ${resourceDefinition.aliases.size} aliases for ${token.asString}"
      )
    }

    val resourceBaseProperties = Seq(
      "urn" -> PropertyDefinition(typeReference = UrnType),
      "id" -> PropertyDefinition(typeReference = ResourceIdType)
    )
    // Some property names are reserved for resource outputs
    resourceDefinition.properties.foreach {
      case (name, _) => {
        if (name == "urn" || name == "id") {
          // TODO: throw an exception once 'kubernetes' is fixed: https://github.com/pulumi/pulumi-kubernetes/issues/2683
          logger.error(s"invalid property for '${token.asString}': property name '${name}' is reserved")
        }
      }
    }

    val requiredOutputs = (resourceDefinition.required ++ resourceBaseProperties.toMap.keys).toSet

    val resourceProperties = {
      val allProperties = (resourceBaseProperties ++ resourceDefinition.properties.toSeq.sortBy(_._1))
      val truncatedProperties =
        if (allProperties.size <= jvmMaxParamsCount) allProperties
        else {
          logger.warn(
            s"Resource type ${token.asString} has too many properties. Only first ${jvmMaxParamsCount} will be kept"
          )
          allProperties.take(jvmMaxParamsCount)
        }
      truncatedProperties.map { case (propertyName, propertyDefinition) =>
        PropertyInfo.from(
          propertyName = propertyName,
          propertyDefinition = propertyDefinition,
          isPropertyRequired = requiredOutputs.contains(propertyName)
        )
      }
    }

    val requiredInputs = resourceDefinition.requiredInputs.toSet

    val inputProperties = {
      val allProperties = resourceDefinition.inputProperties.toSeq.sortBy(_._1)
      val truncatedProperties =
        if (allProperties.size <= jvmMaxParamsCount) allProperties
        else {
          logger.warn(
            s"Resource type ${token.asString} has too many input properties. Only first ${jvmMaxParamsCount} will be kept"
          )
          allProperties.take(jvmMaxParamsCount)
        }
      truncatedProperties.map { case (propertyName, propertyDefinition) =>
        PropertyInfo.from(
          propertyName = propertyName,
          propertyDefinition = propertyDefinition,
          isPropertyRequired = requiredInputs.contains(propertyName)
        )
      }
    }

    val stateInputs = resourceDefinition.stateInputs.properties.toSeq.sortBy(_._1)
    if (stateInputs.nonEmpty) {
      logger.warn(s"State inputs are not supported yet, ignoring ${stateInputs.size} state inputs for ${token.asString}")
    }

    val baseClassParams = resourceProperties.map { propertyInfo =>
      val innerType =
        if propertyInfo.isOptional
        then scalameta.types.Option(propertyInfo.baseType)
        else propertyInfo.baseType
      Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(scalameta.types.besom.types.Output(innerType)),
        default = None
      )
    }

    val baseOutputExtensionMethods: Seq[Stat] = resourceProperties.map { propertyInfo =>
      val innerType =
        if propertyInfo.isOptional
        then scalameta.types.Option(propertyInfo.baseType)
        else propertyInfo.baseType
      val resultType = scalameta.types.besom.types.Output(innerType)

      // colon after underscore would be treated as a part of the name so we add a space
      m"""def ${propertyInfo.name} : $resultType = output.flatMap(_.${propertyInfo.name})""".parse[Stat].get
    }

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Implement comments
    val baseClassComment = ""

    val resourceBaseClass =
      if isProvider
      then "besom.ProviderResource".parse[Type].get
      else "besom.CustomResource".parse[Type].get

    if (resourceDefinition.isComponent) {
      logger.warn(s"Component resources are not supported yet, generating incorrect resource ${token.asString}")
    }

    val baseClass =
      m"""|final case class $baseClassName private(
          |${baseClassParams.map(param => s"  ${param.syntax}").mkString(",\n")}
          |) extends ${resourceBaseClass}""".stripMargin.parse[Stat].get

    val (methodFiles, baseClassMethods) =
      methods.map { case (name, (functionCoordinates, functionDefinition)) =>
        val functionToken     = functionCoordinates.token
        val methodCoordinates = functionCoordinates.resourceMethod
        if (!methodCoordinates.definitionName.contains(name)) {
          logger.warn(
            s"""|Resource definition method name '${name}' (used) does not match the name
                |in method definition '${methodCoordinates.definitionName}' (ignored)
                |for function '${functionToken.asString}' - this is a schema error""".stripMargin
          )
        }
        val (supportClassSourceFiles, argsClassRef, argsDefault, resultTypeRef) =
          functionSupport(functionCoordinates, functionDefinition)

        val thisTypeRef  = baseClassCoordinates.typeRef
        val tokenLiteral = Lit.String(functionToken.asString)
        val code =
          m"""|  def ${Term.Name(name)}(using ctx: besom.types.Context)(
              |    args: ${argsClassRef}${argsDefault},
              |    opts: besom.InvokeOptions = besom.InvokeOptions()
              |  ): besom.types.Output[${resultTypeRef}] =
              |     ctx.call[$argsClassRef, $resultTypeRef, $thisTypeRef](${tokenLiteral}, args, this, opts)
              |""".stripMargin.parse[Stat].get

        (supportClassSourceFiles, code)
      }.unzip

    val hasDefaultArgsConstructor = requiredInputs.forall { propertyName =>
      val propertyDefinition = resourceDefinition.inputProperties(propertyName)
      propertyDefinition.default.nonEmpty || propertyDefinition.const.nonEmpty
    }

    val argsDefault = if (hasDefaultArgsConstructor) s""" = ${argsClassName}()""" else ""

    // the token has to match Pulumi's resource type schema, e.g. kubernetes:core/v1:Pod
    // please make sure, it contains 'index' instead of empty module part if needed
    val tokenLit = Lit.String(token.asString)

    val resourceDecoderInstance =
      m"""  given resourceDecoder(using besom.types.Context): besom.types.ResourceDecoder[$baseClassName] =
         |    besom.internal.ResourceDecoder.derived[$baseClassName]
         |""".stripMargin

    val decoderInstance =
      m"""  given decoder(using besom.types.Context): besom.types.Decoder[$baseClassName] =
         |    besom.internal.Decoder.customResourceDecoder[$baseClassName]
         |""".stripMargin

    val baseCompanion =
      if (hasOutputExtensions) {
        m"""|object $baseClassName:
            |  def apply(using ctx: besom.types.Context)(
            |    name: besom.util.NonEmptyString,
            |    args: ${argsClassName}${argsDefault},
            |    opts: besom.CustomResourceOptions = besom.CustomResourceOptions()
            |  ): besom.types.Output[$baseClassName] =
            |    ctx.readOrRegisterResource[$baseClassName, $argsClassName](${tokenLit}, name, args, opts)
            |
            |$resourceDecoderInstance
            |$decoderInstance
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"      ${meth.syntax}").mkString("\n")}
            |""".stripMargin.parse[Stat].get
      } else {
        m"""object $baseClassName:
           |$resourceDecoderInstance""".stripMargin.parse[Stat].get
      }

    val baseClassFileContent =
      m"""|package ${baseClassCoordinates.packageRef}
          |${baseClassComment}
          |${baseClass}${if (baseClassMethods.nonEmpty) ":" else ""}
          |${baseClassMethods.mkString("\n")}
          |${baseCompanion}
          |""".stripMargin.parse[Source].get

    val baseClassSourceFile = SourceFile(
      baseClassCoordinates.filePath,
      baseClassFileContent.syntax
    )

    val argsClassSourceFile = makeArgsClassSourceFile(
      classCoordinates = argsClassCoordinates,
      properties = inputProperties,
      isResource = true,
      isProvider = isProvider
    )

    Seq(baseClassSourceFile, argsClassSourceFile) ++ methodFiles.flatten
  }

  def sourceFilesForFunctions(pulumiPackage: PulumiPackage)(implicit logger: Logger): Seq[SourceFile] = {
    pulumiPackage.parsedFunctions
      .filterNot { case (_, f) => isMethod(f) }
      .map { case (coordinates, functionDefinition) =>
        sourceFilesForFunction(
          functionCoordinates = coordinates,
          functionDefinition = functionDefinition
        )
      }
      .toSeq
      .flatten
  }

  private def functionSupport(
    functionCoordinates: PulumiDefinitionCoordinates,
    functionDefinition: FunctionDefinition
  ): (Seq[SourceFile], Type.Ref, String, Type) = {
    val requiredInputs = functionDefinition.inputs.required
    val inputProperties =
      functionDefinition.inputs.properties.toSeq
        .sortBy(_._1)
        .filterNot {
          case (Utils.selfParameterName, _) => true
          case _                            => false
        }
        .collect { case (propertyName, propertyDefinition) =>
          PropertyInfo.from(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = requiredInputs.contains(propertyName)
          )
        }

    val argsClassCoordinates = functionCoordinates.methodArgsClass
    val argsClassSourceFile = makeArgsClassSourceFile(
      classCoordinates = argsClassCoordinates,
      properties = inputProperties,
      isResource = false,
      isProvider = false
    )

    val resultClassCoordinates = functionCoordinates.methodResultClass
    val resultClassSourceFileOpt = functionDefinition.outputs.objectTypeDefinition.map { outputTypeDefinition =>
      val requiredOutputs = outputTypeDefinition.required
      val outputProperties =
        outputTypeDefinition.properties.toSeq.sortBy(_._1).map { case (propertyName, propertyDefinition) =>
          PropertyInfo.from(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = requiredOutputs.contains(propertyName)
          )
        }

      makeOutputClassSourceFile(
        classCoordinates = resultClassCoordinates,
        properties = outputProperties
      )
    }

    val argsClassRef = argsClassCoordinates.typeRef
    val argsDefault =
      if (inputProperties.isEmpty) {
        s" = ${argsClassRef}()"
      } else ""

    val resultTypeRef: Type =
      (functionDefinition.outputs.objectTypeDefinition, functionDefinition.outputs.typeReference) match {
        case (Some(_), _) =>
          resultClassCoordinates.typeRef
        case (None, Some(ref)) =>
          ref.asScalaType()
        case (None, None) =>
          scalameta.types.Unit
      }

    (Seq(argsClassSourceFile) ++ resultClassSourceFileOpt, argsClassRef, argsDefault, resultTypeRef)
  }

  def sourceFilesForFunction(
    functionCoordinates: PulumiDefinitionCoordinates,
    functionDefinition: FunctionDefinition
  ): Seq[SourceFile] = {
    val (supportClassSourceFiles, argsClassRef, argsDefault, resultTypeRef) = functionSupport(
      functionCoordinates,
      functionDefinition
    )
    val functionToken     = functionCoordinates.token
    val methodCoordinates = functionCoordinates.resourceMethod

    val methodName = methodCoordinates.selectionName.getOrElse(
      throw GeneralCodegenException(
        s"Function '${functionToken.asString}' does not have a selection name"
      )
    )
    val methodSourceFile = {
      val token = Lit.String(functionToken.asString)

      val fileContent =
        s"""|package ${methodCoordinates.packageRef}
            |
            |def ${Name(methodName)}(using ctx: besom.types.Context)(
            |  args: ${argsClassRef}${argsDefault},
            |  opts: besom.InvokeOptions = besom.InvokeOptions()
            |): besom.types.Output[${resultTypeRef}] =
            |   ctx.invoke[$argsClassRef, $resultTypeRef](${token}, args, opts)
            |""".stripMargin

      SourceFile(filePath = methodCoordinates.filePath, sourceCode = fileContent)
    }

    supportClassSourceFiles :+ methodSourceFile
  }

  def sourceFilesForConfig(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    pulumiPackage.config.variables
      .collect { case (prop, propDefinition) =>
        logger.warn(s"Config '${prop}' was not generated")
        Seq() // TODO: implement
      }
      .toSeq
      .flatten
  }

  private def makeOutputClassSourceFile(
    classCoordinates: ScalaDefinitionCoordinates,
    properties: Seq[PropertyInfo]
  ) = {
    val classParams = properties.map { propertyInfo =>
      val fieldType =
        if propertyInfo.isOptional
        then scalameta.types.Option(propertyInfo.baseType)
        else propertyInfo.baseType
      Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(fieldType),
        default = None
      )
    }
    val outputExtensionMethods = properties.map { propertyInfo =>
      val innerType =
        if propertyInfo.isOptional
        then scalameta.types.Option(propertyInfo.baseType)
        else propertyInfo.baseType

      // colon after underscore would be treated as a part of the name so we add a space
      m"""def ${propertyInfo.name} : besom.types.Output[$innerType] = output.map(_.${propertyInfo.name})""".parse[Stat].get
    }
    val optionOutputExtensionMethods = properties.map { propertyInfo =>
      val innerMethodName =
        if propertyInfo.isOptional
        then Term.Name("flatMap")
        else Term.Name("map")

      // colon after underscore would be treated as a part of the name so we add a space
      m"""def ${propertyInfo.name} : besom.types.Output[scala.Option[${propertyInfo.baseType}]] = output.map(_.${innerMethodName}(_.${propertyInfo.name}))"""
        .parse[Stat]
        .get
    }

    val hasOutputExtensions = outputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val classComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val classComment = ""

    val className = classCoordinates.definitionTypeName.getOrElse(
      throw GeneralCodegenException(
        s"Class name for ${classCoordinates.typeRef} could not be found"
      )
    )
    val classDef =
      m"""|final case class ${className} private(
          |${classParams.map(arg => m"  ${arg.syntax}").mkString(",\n")}
          |)""".stripMargin
        .parse[Stat]
        .fold(
          e =>
            throw GeneralCodegenException(
              s"Failed to parse class definition for ${classCoordinates.typeRef}: ${e}",
              e.details
            ),
          identity
        )

    val decoderInstance =
      m"""  given decoder(using besom.types.Context): besom.types.Decoder[$className] =
         |    besom.internal.Decoder.derived[$className]
         |""".stripMargin

    val baseCompanionDef =
      if (hasOutputExtensions) {
        // colon after underscore would be treated as a part of the name so we add a space
        m"""|object ${className} :
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[$className])
            |${outputExtensionMethods.map(meth => m"      ${meth.syntax}").mkString("\n")}
            |
            |$decoderInstance
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[$className]])
            |${optionOutputExtensionMethods.map(meth => m"      ${meth.syntax}").mkString("\n")}
            |""".stripMargin.parse[Stat].get
      } else {
        m"""object $className:
           |$decoderInstance""".stripMargin.parse[Stat].get
      }

    val fileContent =
      m"""|package ${classCoordinates.packageRef}
          |
          |${classComment}
          |${classDef}
          |
          |${baseCompanionDef}
          |
          |""".stripMargin.parse[Source].get

    SourceFile(
      classCoordinates.filePath,
      fileContent.syntax
    )
  }

  private def makeArgsClassSourceFile(
    classCoordinates: ScalaDefinitionCoordinates,
    properties: Seq[PropertyInfo],
    isResource: Boolean,
    isProvider: Boolean
  ) = {
    val argsClass = makeArgsClass(
      classCoordinates = classCoordinates,
      inputProperties = properties
    )
    val argsCompanion = makeArgsCompanion(
      classCoordinates = classCoordinates,
      inputProperties = properties,
      isResource = isResource,
      isProvider = isProvider
    )

    val fileContent =
      m"""|package ${classCoordinates.packageRef}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin.parse[Source].get

    SourceFile(
      classCoordinates.filePath,
      fileContent.syntax
    )
  }

  private def makeArgsClass(
    classCoordinates: ScalaDefinitionCoordinates,
    inputProperties: Seq[PropertyInfo]
  ): Stat = {
    val argsClassName = classCoordinates.definitionTypeName.getOrElse(
      throw GeneralCodegenException(
        s"Class name for ${classCoordinates.typeRef} could not be found"
      )
    )
    val argsClassParams = inputProperties.map { propertyInfo =>
      val fieldType =
        if propertyInfo.isOptional
        then scalameta.types.Option(propertyInfo.argType)
        else propertyInfo.argType
      Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(scalameta.types.besom.types.Output(fieldType)),
        default = None
      )
    }

    m"""|final case class $argsClassName private(
        |${argsClassParams.map(arg => m"  ${arg.syntax}").mkString(",\n")}
        |)""".stripMargin.parse[Stat].get
  }

  private def makeArgsCompanion(
    classCoordinates: ScalaDefinitionCoordinates,
    inputProperties: Seq[PropertyInfo],
    isResource: Boolean,
    isProvider: Boolean
  ): Stat = {
    val argsClassName = classCoordinates.definitionTypeName.getOrElse(
      throw GeneralCodegenException(
        s"Class name for ${classCoordinates.typeRef} could not be found"
      )
    )
    val argsCompanionApplyParams = inputProperties.filter(_.constValue.isEmpty).map { propertyInfo =>
      val paramType =
        if (propertyInfo.isOptional) scalameta.types.besom.types.InputOptional(propertyInfo.inputArgType)
        else scalameta.types.besom.types.Input(propertyInfo.inputArgType)
      Term.Param(
        mods = List.empty,
        name = propertyInfo.name,
        decltpe = Some(paramType),
        default = propertyInfo.defaultValue
      )
    }

    val argsCompanionApplyBodyArgs = inputProperties.map { propertyInfo =>
      val isSecret = Lit.Boolean(propertyInfo.isSecret)
      val argValue = propertyInfo.constValue match {
        case Some(constValue) =>
          scalameta.besom.types.Output(constValue)
        case None =>
          if (propertyInfo.isOptional)
            m"${propertyInfo.name}.asOptionOutput(isSecret = ${isSecret})".parse[Term].get
          else
            m"${propertyInfo.name}.asOutput(isSecret = ${isSecret})".parse[Term].get
      }
      Term.Assign(Term.Name(propertyInfo.name.value), argValue)
    }

    val derivedTypeclasses = {
      lazy val providerArgsEncoderInstance =
        m"""  given encoder(using besom.types.Context): besom.types.ProviderArgsEncoder[$argsClassName] =
           |    besom.internal.ProviderArgsEncoder.derived[$argsClassName]
           |""".stripMargin

      lazy val argsEncoderInstance =
         m"""  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[$argsClassName] =
            |    besom.internal.ArgsEncoder.derived[$argsClassName]
            |""".stripMargin

      if (isProvider) providerArgsEncoderInstance
      else if (isResource)
        m"""  given encoder(using besom.types.Context): besom.types.Encoder[$argsClassName] =
           |    besom.internal.Encoder.derived[$argsClassName]
           |$argsEncoderInstance""".stripMargin
      else m"""  given encoder(using besom.types.Context): besom.types.Encoder[$argsClassName] =
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
        |$derivedTypeclasses
        |""".stripMargin.parse[Stat].get
  }
}

case class FilePath(pathParts: Seq[String]) {
  require(
    pathParts.forall(!_.contains('/')),
    s"Path parts cannot contain '/', got: ${pathParts.mkString("[", ",", "]")}"
  )

  def osSubPath: os.SubPath = pathParts.foldLeft(os.sub)(_ / _)
}
object FilePath {
  def apply(path: String): FilePath               = new FilePath(os.SubPath(path).segments)
  def unapply(filePath: FilePath): Option[String] = Some(filePath.pathParts.mkString("/"))
}

case class SourceFile(
  filePath: FilePath,
  sourceCode: String
)
