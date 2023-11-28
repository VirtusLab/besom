package besom.codegen

import besom.codegen.Config.{CodegenConfig, ProviderConfig}
import besom.codegen.PackageVersion.PackageVersion
import besom.codegen.PulumiTypeReference.TypeReferenceOps
import besom.codegen.Utils._
import besom.codegen.metaschema._

import scala.meta._
import scala.meta.dialects.Scala33

//noinspection ScalaWeakerAccess,TypeAnnotation
class CodeGen(implicit
  codegenConfig: CodegenConfig,
  providerConfig: ProviderConfig,
  thisPackageInfo: ThisPackageInfo,
  schemaProvider: SchemaProvider,
  logger: Logger
) {

  def sourcesFromPulumiPackage(
    pulumiPackage: PulumiPackage,
    packageInfo: PulumiPackageInfo
  ): Seq[SourceFile] =
    scalaFiles(pulumiPackage, packageInfo) ++ Seq(
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

  def scalaFiles(
    pulumiPackage: PulumiPackage,
    packageInfo: PulumiPackageInfo
  ): Seq[SourceFile] = {
    val (configFiles, configDependencies) = sourceFilesForConfig(pulumiPackage, packageInfo)
    configFiles ++
      sourceFilesForProviderResource(pulumiPackage) ++
      sourceFilesForNonResourceTypes(pulumiPackage, configDependencies) ++
      sourceFilesForResources(pulumiPackage) ++
      sourceFilesForFunctions(pulumiPackage)
  }

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
    val typeToken = PulumiToken(pulumiPackage.providerTypeToken)
    sourceFilesForResource(
      typeCoordinates = typeToken.toCoordinates(pulumiPackage),
      resourceDefinition = pulumiPackage.provider,
      isProvider = true
    )
  }

  def sourceFilesForNonResourceTypes(pulumiPackage: PulumiPackage, configDependencies: Seq[ConfigDependency]): Seq[SourceFile] = {
    pulumiPackage.types.flatMap { case (typeToken, typeDefinition) =>
      val typeCoordinates = PulumiToken(typeToken).toCoordinates(pulumiPackage)

      typeDefinition match {
        case enumDef: EnumTypeDefinition =>
          sourceFilesForEnum(typeCoordinates = typeCoordinates, enumDefinition = enumDef)
        case objectDef: ObjectTypeDefinition =>
          sourceFilesForObjectType(
            typeCoordinates = typeCoordinates,
            objectTypeDefinition = objectDef,
            configDependencies = configDependencies
          )
      }
    }.toSeq
  }

  def sourceFilesForEnum(
    typeCoordinates: PulumiDefinitionCoordinates,
    enumDefinition: EnumTypeDefinition
  ): Seq[SourceFile] = {
    val classCoordinates = typeCoordinates.asEnumClass

    val enumClassName       = Type.Name(classCoordinates.definitionName).syntax
    val enumClassStringName = Lit.String(classCoordinates.definitionName).syntax

    val (superclass, valueType) = enumDefinition.`type` match {
      case BooleanType => ("besom.types.BooleanEnum", "Boolean")
      case IntegerType => ("besom.types.IntegerEnum", "Int")
      case NumberType  => ("besom.types.NumberEnum", "Double")
      case StringType  => ("besom.types.StringEnum", "String")
    }

    val instances: Seq[(String, String)] = enumDefinition.`enum`.map { valueDefinition =>
      val caseRawName = valueDefinition.name.getOrElse {
        valueDefinition.value match {
          case StringConstValue(value) => value
          case const                   => throw GeneralCodegenError(s"The name of enum cannot be derived from value ${const}")
        }
      }
      val caseName       = Term.Name(caseRawName).syntax
      val caseStringName = Lit.String(caseRawName).syntax
      val caseValue      = valueDefinition.value.asScala.syntax

      val definition = s"""object ${caseName} extends ${enumClassName}(${caseStringName}, ${caseValue})"""
      val reference  = caseName
      (definition, reference)
    }

    val fileContent =
      s"""|package ${classCoordinates.packageRef.syntax}
          |
          |sealed abstract class ${enumClassName}(val name: String, val value: ${valueType}) extends ${superclass}
          |
          |object ${enumClassName} extends besom.types.EnumCompanion[${valueType}, ${enumClassName}](${enumClassStringName}):
          |${instances.map(instance => s"  ${instance._1}").mkString("\n")}
          |
          |  override val allInstances: Seq[${enumClassName}] = Seq(
          |${instances.map(instance => s"    ${instance._2}").mkString(",\n")}
          |  )
          |""".stripMargin

    Seq(
      SourceFile(
        classCoordinates.filePath,
        fileContent
      )
    )
  }

  def sourceFilesForObjectType(
    typeCoordinates: PulumiDefinitionCoordinates,
    objectTypeDefinition: ObjectTypeDefinition,
    configDependencies: Seq[ConfigDependency]
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
        PropertyInfo
          .from(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = objectTypeDefinition.required.contains(propertyName)
          )
          .toTry
          .get
      }
    }

    // Config types need to be serialized to JSON for structured configs
    val outputRequiresJsonFormat = typeToken.module == Utils.configModuleName | {
      configDependencies.map(_.coordinates.token.asLookupKey).contains(typeToken.asLookupKey)
    }

    val baseClassSourceFile = makeOutputClassSourceFile(
      baseClassCoordinates,
      objectProperties,
      requiresJsonFormat = outputRequiresJsonFormat
    )

    val argsClassSourceFile = makeArgsClassSourceFile(
      classCoordinates = argsClassCoordinates,
      properties = objectProperties,
      isResource = false,
      isProvider = false
    )

    Seq(baseClassSourceFile, argsClassSourceFile)
  }

  def sourceFilesForResources(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    pulumiPackage.resources
      .collect {
        case (typeToken, resourceDefinition) if !resourceDefinition.isOverlay =>
          sourceFilesForResource(
            typeCoordinates = PulumiToken(typeToken).toCoordinates(pulumiPackage),
            resourceDefinition = resourceDefinition,
            isProvider = false
          )
      }
      .toSeq
      .flatten
  }

  def sourceFilesForResource(
    typeCoordinates: PulumiDefinitionCoordinates,
    resourceDefinition: ResourceDefinition,
    isProvider: Boolean
  ): Seq[SourceFile] = {
    val typeToken            = typeCoordinates.token
    val baseClassCoordinates = typeCoordinates.asResourceClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asResourceClass(asArgsType = true)
    val baseClassName        = Type.Name(baseClassCoordinates.definitionName).syntax
    val argsClassName        = Type.Name(argsClassCoordinates.definitionName).syntax

    if (resourceDefinition.aliases.nonEmpty) {
      logger.warn(
        s"Aliases are not supported yet, ignoring ${resourceDefinition.aliases.size} aliases for ${typeToken.asString}"
      )
    }

    val resourceBaseProperties = Seq(
      "urn" -> PropertyDefinition(typeReference = UrnType),
      "id" -> PropertyDefinition(typeReference = ResourceIdType)
    )

    val requiredOutputs = (resourceDefinition.required ++ List("urn", "id")).toSet

    val resourceProperties = {
      val allProperties = (resourceBaseProperties ++ resourceDefinition.properties.toSeq.sortBy(_._1))
      val truncatedProperties =
        if (allProperties.size <= jvmMaxParamsCount) allProperties
        else {
          logger.warn(
            s"Resource type ${typeToken.asString} has too many properties. Only first ${jvmMaxParamsCount} will be kept"
          )
          allProperties.take(jvmMaxParamsCount)
        }
      truncatedProperties.map { case (propertyName, propertyDefinition) =>
        PropertyInfo
          .from(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = requiredOutputs.contains(propertyName)
          )
          .toTry
          .get
      }
    }

    val requiredInputs = resourceDefinition.requiredInputs.toSet

    val inputProperties = {
      val allProperties = resourceDefinition.inputProperties.toSeq.sortBy(_._1)
      val truncatedProperties =
        if (allProperties.size <= jvmMaxParamsCount) allProperties
        else {
          logger.warn(
            s"Resource type ${typeToken.asString} has too many input properties. Only first ${jvmMaxParamsCount} will be kept"
          )
          allProperties.take(jvmMaxParamsCount)
        }
      truncatedProperties.map { case (propertyName, propertyDefinition) =>
        PropertyInfo
          .from(
            propertyName = propertyName,
            propertyDefinition = propertyDefinition,
            isPropertyRequired = requiredInputs.contains(propertyName)
          )
          .toTry
          .get
      }
    }

    val stateInputs = resourceDefinition.stateInputs.properties.toSeq.sortBy(_._1)
    if (stateInputs.nonEmpty) {
      logger.warn(s"State inputs are not supported yet, ignoring ${stateInputs.size} state inputs for ${typeToken.asString}")
    }

    val baseClassParams            = resourceProperties.map(_.asOutputParam)
    val baseOutputExtensionMethods = resourceProperties.map(_.asScalaParam)

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val resourceBaseClass = if (isProvider) "besom.ProviderResource" else "besom.CustomResource"
    if (resourceDefinition.isComponent) {
      logger.warn(s"Component resources are not supported yet, generating incorrect resource ${typeToken.asString}")
    }

    val baseClass =
      s"""|final case class $baseClassName private(
          |${baseClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives besom.ResourceDecoder""".stripMargin

    val hasDefaultArgsConstructor = requiredInputs.forall { propertyName =>
      val propertyDefinition = resourceDefinition.inputProperties(propertyName)
      propertyDefinition.default.nonEmpty || propertyDefinition.const.nonEmpty
    }

    val argsDefault = if (hasDefaultArgsConstructor) s""" = ${argsClassName}()""" else ""

    // the token has to match Pulumi's resource type schema, e.g. kubernetes:core/v1:Pod
    // please make sure, it contains 'index' instead of empty module part if needed
    val token = Lit.String(typeToken.asString)

    val baseCompanion =
      if (hasOutputExtensions) {
        s"""|object $baseClassName:
            |  def apply(using ctx: besom.types.Context)(
            |    name: besom.util.NonEmptyString,
            |    args: ${argsClassName}${argsDefault},
            |    opts: besom.CustomResourceOptions = besom.CustomResourceOptions()
            |  ): besom.types.Output[$baseClassName] =
            |    ctx.registerResource[$baseClassName, $argsClassName](${token}, name, args, opts)
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"      ${meth.syntax}").mkString("\n")}""".stripMargin
      } else {
        s"""object $baseClassName"""
      }

    val baseClassFileContent =
      s"""|package ${baseClassCoordinates.packageRef}
          |
          |${baseClassComment}
          |${baseClass}
          |
          |${baseCompanion}
          |""".stripMargin

      val baseClassSourceFile = SourceFile(
        baseClassCoordinates.filePath,
        baseClassFileContent
      )

      val argsClassSourceFile = makeArgsClassSourceFile(
        classCoordinates = argsClassCoordinates,
        properties = inputProperties,
        isResource = true,
        isProvider = isProvider
      )

      Seq(baseClassSourceFile, argsClassSourceFile)
  }

  def sourceFilesForFunctions(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    val moduleToPackageParts   = pulumiPackage.moduleToPackageParts
    val providerToPackageParts = pulumiPackage.providerToPackageParts

    pulumiPackage.functions
      .collect {
        case (functionToken, functionDefinition) if !functionDefinition.isOverlay =>
          sourceFilesForFunction(
            functionCoordinates = PulumiDefinitionCoordinates.fromRawToken(functionToken, moduleToPackageParts, providerToPackageParts),
            functionDefinition = functionDefinition,
            functionToken = PulumiToken(functionToken)
          )
      }
      .toSeq
      .flatten
  }

  def sourceFilesForFunction(
    functionCoordinates: PulumiDefinitionCoordinates,
    functionDefinition: FunctionDefinition,
    functionToken: PulumiToken
  ): Seq[SourceFile] = {
    val isMethod = functionDefinition.inputs.properties.isDefinedAt(Utils.selfParameterName)

    if (isMethod) {
      logger.warn(s"Function '${functionToken.asString}' was not generated - methods are not yet supported")
      Seq.empty
    } else {
      val methodCoordinates = functionCoordinates.topLevelMethod

      val methodName = methodCoordinates.definitionName
      if (methodName.contains("/")) {
        throw GeneralCodegenError(s"Top level function name ${methodName} containing a '/' is not allowed")
      }

      val requiredInputs = functionDefinition.inputs.required
      val inputProperties =
        functionDefinition.inputs.properties.toSeq.sortBy(_._1).collect { case (propertyName, propertyDefinition) =>
          PropertyInfo
            .from(
              propertyName = propertyName,
              propertyDefinition = propertyDefinition,
              isPropertyRequired = requiredInputs.contains(propertyName)
            )
            .toTry
            .get
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
            PropertyInfo
              .from(
                propertyName = propertyName,
                propertyDefinition = propertyDefinition,
                isPropertyRequired = requiredOutputs.contains(propertyName)
              )
              .toTry
              .get
          }

        makeOutputClassSourceFile(
          classCoordinates = resultClassCoordinates,
          properties = outputProperties,
          requiresJsonFormat = false
        )
      }

      val argsClassRef: Type.Ref = argsClassCoordinates.typeRef
      val argsClassParamName = Term.Name("args")
      val argsClassParam: Term.Param = argsClassCoordinates.asNamedParam(argsClassParamName)(withDefault = inputProperties.isEmpty)

      val resultTypeRef: Type =
        (functionDefinition.outputs.objectTypeDefinition, functionDefinition.outputs.typeReference) match {
          case (Some(_), _) => resultClassCoordinates.typeRef
          case (None, Some(ref)) => ref.asScalaType().toTry.get
          case (None, None) => scalameta.types.Unit
        }

      val methodSourceFile = {
        val token = Lit.String(functionToken.asString)

        val fileContent =
          s"""|package ${methodCoordinates.packageRef.syntax}
              |
              |def ${Term.Name(methodName)}(using ctx: besom.types.Context)(
              |  ${argsClassParam.syntax},
              |  opts: besom.InvokeOptions = besom.InvokeOptions()
              |): ${scalameta.types.besom.types.Output(resultTypeRef)} =
              |   ctx.invoke[${argsClassRef.syntax}, ${resultTypeRef.syntax}](${token.syntax}, ${argsClassParamName.syntax}, opts)
              |""".stripMargin

        SourceFile(
          filePath = methodCoordinates.filePath,
          sourceCode = fileContent
        )
      }

      Seq(argsClassSourceFile, methodSourceFile) ++ resultClassSourceFileOpt
    }
  }

  def sourceFilesForConfig(pulumiPackage: PulumiPackage, packageInfo: PulumiPackageInfo): (Seq[SourceFile], Seq[ConfigDependency]) = {
    val PulumiToken(_, _, providerName) = PulumiToken(packageInfo.providerTypeToken)
    val coordinates = PulumiToken(providerName, Utils.configModuleName, Utils.configTypeName)
      .toCoordinates(pulumiPackage)
      .asConfigClass

    val configsWithDefault = pulumiPackage.config.defaults
    val configVariables    = pulumiPackage.config.variables.toSeq.sortBy(_._1)
    val configs = configVariables.collect { case (configName, configDefinition) =>
      val get      = s"get${configName.capitalize}"
      val propType = configDefinition.typeReference.asScalaType().toTry.get
      val isSecret = configDefinition.secret
      val defaultValue = configDefinition.default.map(_.asScala).filter {
        case Lit.String(v) if v.isBlank => false
        case _                          => true
      }
      val default = defaultValue match {
        case Some(value) => scalameta.Some(value)
        case None        => scalameta.None
      }
      val environmentValues = configDefinition.defaultInfo.toList.flatMap(_.environment)
      val environment       = scalameta.List(environmentValues.map(Lit.String(_)))
      if (configsWithDefault.contains(configName) && environmentValues.isEmpty && defaultValue.isEmpty) {
        logger.warn(
          s"Config '${configName}' should have defaults but none were found - schema error."
        )
      }
      val description = configDefinition.description.getOrElse("")
      val deprecationCode = configDefinition.deprecationMessage match {
        case Some(message) => s"""\n  @deprecated("$message")"""
        case None          => ""
      }
      val deprecationDocs = configDefinition.deprecationMessage match {
        case Some(message) => s"""\n   * @deprecated $message"""
        case None          => ""
      }
      val returnsDoc = s"@returns the value of the `$providerName:$configName` configuration property" +
        s"${if (configsWithDefault.contains(configName)) " or a default value if present" else ""}."

      s"""|/**
          | * ${description}${deprecationDocs}
          | * ${returnsDoc}
          | */${deprecationCode}
          |def ${Term.Name(get)}(using ${scalameta.types.besom.types.Context}): ${scalameta.types.besom.types.Output(
           scalameta.types.Option(propType)
         )} =
          |  besom.internal.Codegen.config[${propType.syntax}](${Lit.String(providerName)})(key = ${Lit.String(
           configName
         )}, isSecret = ${Lit.Boolean(isSecret)}, default = ${default.syntax}, environment = ${environment.syntax})
          |""".stripMargin
    }

    val code =
      s"""|${scalameta.package_(coordinates.packageRef)()}
          |${scalameta.importAll(scalameta.besom.internal.CodegenProtocol())}
          |${configs.mkString("\n")}
          |""".stripMargin

    if (pulumiPackage.config.variables.isEmpty) {
      (Seq.empty, Seq.empty)
    } else {
      val file    = FilePath(Seq("src", Utils.configModuleName, "config.scala"))
      val sources = Seq(SourceFile(file, code))
      val dependencies = configVariables
        .collect({ case (_, configDefinition) =>
          configDefinition.typeReference.asPulumiTypeReference match {
            case InternalTypeReference(token, _, _) => ConfigSourceDependency(token.toCoordinates(pulumiPackage)) :: Nil
            case ExternalTypeReference(token, _, _, metadata) =>
              ConfigRuntimeDependency(token.toCoordinates(pulumiPackage), metadata) :: Nil
            case _ => Nil
          }
        })

      (sources, dependencies.flatten)
    }
  }

  private def makeOutputClassSourceFile(
    classCoordinates: ScalaDefinitionCoordinates,
    properties: Seq[PropertyInfo],
    requiresJsonFormat: Boolean
  ): SourceFile = {
    val className   = classCoordinates.definitionName
    val typeName    = classCoordinates.typeName
    val classParams = properties.map(_.asScalaParam)
    val jsonFormatExtensionMethod = Option.when(requiresJsonFormat) {
      scalameta.given_(scalameta.types.spray.json.JsonFormat(typeName)) {
        scalameta.besom.internal.CodegenProtocol.jsonFormatN(properties.size)(scalameta.apply_(typeName))
      }
    }

    val outputExtensionMethods       = properties.map(_.asScalaGetter)
    val optionOutputExtensionMethods = properties.map(_.asScalaOptionGetter)

    val hasOutputExtensions = outputExtensionMethods.nonEmpty

    // TODO: Add comments

    val classDef = scalameta.parse(
      s"""|final case class $className private(
          |${classParams.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives besom.types.Decoder
          |""".stripMargin
    )

    val baseCompanionDef = scalameta.parse(
      if (hasOutputExtensions) {
        val classNameTerminator =
          if (className.endsWith("_")) " " else "" // colon after underscore would be treated as a part of the name

        s"""|object ${className}${classNameTerminator}:
            |  ${jsonFormatExtensionMethod.getOrElse("")}
            |  given outputOps: {} with
            |    extension(output: ${scalameta.types.besom.types.Output(typeName)})
            |${outputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}
            |
            |  given optionOutputOps: {} with
            |    extension(output: ${scalameta.types.besom.types.Output(scalameta.types.Option(typeName))})
            |${optionOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}
            |""".stripMargin
      } else {
        s"""object $className"""
      }
    )

    val imports: List[Import] = if (requiresJsonFormat) {
      scalameta.importAll(scalameta.besom.internal.CodegenProtocol()) :: Nil
    } else {
      Nil
    }

    val fileContent = scalameta.package_(classCoordinates.packageRef) {
        imports ++ (classDef :: baseCompanionDef :: Nil)
    }

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
  ): SourceFile = {
    val argsClass = makeArgsClass(
      argsClassName = classCoordinates.definitionName,
      inputProperties = properties,
      isResource = isResource,
      isProvider = isProvider
    )
    val argsCompanion = makeArgsCompanion(
      argsClassName = classCoordinates.definitionName,
      inputProperties = properties,
      isResource = isResource
    )
    val fileContent = scalameta.package_(classCoordinates.packageRef) {
      argsClass :: argsCompanion :: Nil
    }

    SourceFile(
      classCoordinates.filePath,
      fileContent.syntax
    )
  }

  private def makeArgsClass(
    argsClassName: String,
    inputProperties: Seq[PropertyInfo],
    isResource: Boolean,
    isProvider: Boolean
  ): Stat = {
    val derivedTypeclasses =
      if (isProvider) "besom.types.ProviderArgsEncoder"
      else if (isResource) "besom.types.ArgsEncoder"
      else "besom.types.Encoder, besom.types.ArgsEncoder"
    val argsClassParams = inputProperties.map(_.asOutputParam)
    val code =
      s"""|final case class $argsClassName private(
          |${argsClassParams.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives ${derivedTypeclasses}
          |""".stripMargin
    scalameta.parse(code)
  }

  private def makeArgsCompanion(argsClassName: String, inputProperties: Seq[PropertyInfo], isResource: Boolean): Stat = {
    val argsCompanionApplyParams   = inputProperties.filter(_.constValue.isEmpty).map(_.asScalaParam)
    val argsCompanionApplyBodyArgs = inputProperties.map(_.asScalaNamedArg)
    val code =
      s"""|object $argsClassName:
          |  def apply(
          |${argsCompanionApplyParams.map(arg => s"    ${arg}").mkString(",\n")}
          |  )(using besom.types.Context): $argsClassName =
          |    new $argsClassName(
          |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg}").mkString(",\n")}
          |    )
          |""".stripMargin
    scalameta.parse(code)
  }
}

case class FilePath private (pathParts: Seq[String]) {
  require(
    pathParts.forall(!_.contains('/')),
    s"Path parts cannot contain '/', got: ${pathParts.mkString("[", ",", "]")}"
  )

  def osSubPath: os.SubPath = pathParts.foldLeft(os.sub)(_ / _)
}

case class SourceFile(
  filePath: FilePath,
  sourceCode: String
)

/** A dependency that was discovered during code generation and will be required to continue code generation.
  */
sealed trait SourceDependency {
  def coordinates: PulumiDefinitionCoordinates
}

/** A dependency that was discovered during code generation and will be required at runtime.
  */
sealed trait RuntimeDependency extends SourceDependency {
  def metadata: PackageMetadata
}

/** A dependency discovered during [[besom.codegen.metaschema.ConfigDefinition]] parsing.
  */
sealed trait ConfigDependency extends SourceDependency
case class ConfigSourceDependency(coordinates: PulumiDefinitionCoordinates) extends ConfigDependency
case class ConfigRuntimeDependency(coordinates: PulumiDefinitionCoordinates, metadata: PackageMetadata)
    extends ConfigDependency
    with RuntimeDependency
