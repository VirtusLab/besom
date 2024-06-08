package besom.codegen

import besom.codegen.PackageVersion
import besom.codegen.Utils.*
import besom.codegen.metaschema.*
import besom.codegen.scalameta.interpolator.*

import scala.meta.*
import scala.meta.dialects.Scala33

//noinspection ScalaWeakerAccess,TypeAnnotation
class CodeGen(using
  config: Config,
  typeMapper: TypeMapper,
  schemaProvider: SchemaProvider,
  logger: Logger
) {
  import CodeGen.*

  def sourcesFromPulumiPackage(
    packageInfo: PulumiPackageInfo
  ): Seq[SourceFile] =
    scalaFiles(packageInfo) ++ Seq(
      projectConfigFile(
        schemaName = packageInfo.name,
        packageVersion = packageInfo.version
      ),
      resourcePluginMetadataFile(
        pluginName = packageInfo.name,
        pluginVersion = packageInfo.version,
        pluginDownloadUrl = packageInfo.pulumiPackage.pluginDownloadURL
      )
    )

  def scalaFiles(
    packageInfo: PulumiPackageInfo
  ): Seq[SourceFile] = {
    val (configFiles, configDependencies) = sourceFilesForConfig(packageInfo)
    configFiles ++
      sourceFilesForProviderResource(packageInfo) ++
      sourceFilesForNonResourceTypes(packageInfo, configDependencies) ++
      sourceFilesForResources(packageInfo) ++
      sourceFilesForFunctions(packageInfo)
  }

  def projectConfigFile(schemaName: String, packageVersion: PackageVersion): SourceFile = {
    val besomVersion     = config.besomVersion
    val scalaVersion     = config.scalaVersion
    val javaVersion      = config.javaVersion
    val coreShortVersion = config.coreShortVersion
    val organization     = config.organization
    val url              = config.url
    val vcs              = config.vcs
    val license          = config.license
    val repository       = config.repository
    val developers       = config.developers

    val developersBlock = developers.map(developer => s"//> using publish.developer \"$developer\"").mkString("\n")

    val dependencies = packageDependencies(schemaProvider.dependencies(schemaName, packageVersion))

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
          |//> using publish.organization "$organization"
          |//> using publish.version "${packageVersion}-core.${coreShortVersion}"
          |//> using publish.url "$url"
          |//> using publish.vcs "$vcs"
          |//> using publish.license "$license"
          |//> using publish.repository "$repository"
          |${developersBlock}
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

  def sourceFilesForProviderResource(packageInfo: PulumiPackageInfo): Seq[SourceFile] = {
    val typeToken              = packageInfo.providerTypeToken
    val moduleToPackageParts   = packageInfo.moduleToPackageParts
    val providerToPackageParts = packageInfo.providerToPackageParts
    val typeCoordinates =
      PulumiDefinitionCoordinates.fromRawToken(typeToken, moduleToPackageParts, providerToPackageParts)

    given Config.Provider = packageInfo.providerConfig
    sourceFilesForResource(
      typeCoordinates = typeCoordinates,
      resourceDefinition = packageInfo.pulumiPackage.provider,
      methods = packageInfo.parseMethods(packageInfo.pulumiPackage.provider),
      isProvider = true
    )
  }

  def sourceFilesForNonResourceTypes(
    packageInfo: PulumiPackageInfo,
    configDependencies: Seq[ConfigDependency]
  ): Seq[SourceFile] = {
    given Config.Provider = packageInfo.providerConfig

    packageInfo.parsedTypes.flatMap {
      case (coordinates, (enumDef: EnumTypeDefinition, false)) =>
        sourceFilesForEnum(
          typeCoordinates = coordinates,
          enumDefinition = enumDef
        )
      case (coordinates, (_: EnumTypeDefinition, true)) =>
        Overlay.readFiles(
          packageInfo,
          coordinates.token,
          Vector(
            coordinates.asEnumClass
          )
        )
      case (coordinates, (objectDef: ObjectTypeDefinition, false)) =>
        sourceFilesForObjectType(
          typeCoordinates = coordinates,
          objectTypeDefinition = objectDef,
          configDependencies = configDependencies
        )
      case (coordinates, (_: ObjectTypeDefinition, true)) =>
        Overlay.readFiles(
          packageInfo,
          coordinates.token,
          Vector(
            coordinates.asObjectClass(asArgsType = false),
            coordinates.asObjectClass(asArgsType = true)
          )
        )
    }.toSeq
  }

  def sourceFilesForEnum(
    typeCoordinates: PulumiDefinitionCoordinates,
    enumDefinition: EnumTypeDefinition
  )(using Config.Provider): Seq[SourceFile] = {
    val classCoordinates = typeCoordinates.asEnumClass

    val enumClassName = classCoordinates.definitionTypeName.getOrElse(
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

      val definition =
        m"""object ${caseName} extends ${enumClassName}(${caseStringName}, ${caseValue})""".parse[Stat].get
      val reference = caseName
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
          |  given besom.types.EnumCompanion[${valueType}, ${enumClassName}] = this
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
    objectTypeDefinition: ObjectTypeDefinition,
    configDependencies: Seq[ConfigDependency]
  )(using Config.Provider): Seq[SourceFile] = {
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

    // Config types need to be serialized to JSON for structured configs
    val outputRequiresJsonFormat = typeToken.module.startsWith(Utils.configModuleName) | {
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

  def sourceFilesForResources(packageInfo: PulumiPackageInfo): Seq[SourceFile] = {
    given Config.Provider = packageInfo.providerConfig

    packageInfo.parsedResources
      .map {
        case (coordinates, (resourceDefinition, false)) =>
          sourceFilesForResource(
            typeCoordinates = coordinates,
            resourceDefinition = resourceDefinition,
            methods = packageInfo.parseMethods(resourceDefinition),
            isProvider = false
          )
        case (coordinates, (_, true)) =>
          Overlay.readFiles(
            packageInfo,
            coordinates.token,
            Vector(
              coordinates.asResourceClass(asArgsType = false),
              coordinates.asResourceClass(asArgsType = true)
            )
          )
      }
      .toSeq
      .flatten
  }

  def sourceFilesForResource(
    typeCoordinates: PulumiDefinitionCoordinates,
    resourceDefinition: ResourceDefinition,
    methods: Map[FunctionName, (PulumiDefinitionCoordinates, (FunctionDefinition, Boolean))],
    isProvider: Boolean
  )(using Config.Provider): Seq[SourceFile] = {
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
          // TODO: throw an exception once 'kubernetes' is fixed: https://github.com/pulumi/pulumi/issues/15024
          logger.error(s"invalid property for '${token.asString}': property name '${name}' is reserved")
        }
      }
    }

    val requiredOutputs = (resourceDefinition.required ++ resourceBaseProperties.toMap.keys).toSet

    val resourceProperties = {
      val allProperties = resourceBaseProperties ++ resourceDefinition.properties.toSeq.sortBy(_._1)
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
      logger.warn(
        s"State inputs are not supported yet, ignoring ${stateInputs.size} state inputs for ${token.asString}"
      )
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

    val isRemoteComponent = resourceDefinition.isComponent
    val (resourceBaseClass, resourceOptsClass, variant, resourceRegisterMethodName) =
      if isProvider then
        (
          "besom.ProviderResource".parse[Type].get,
          "besom.CustomResourceOptions".parse[Type].get,
          "Custom",
          "readOrRegisterResource"
        )
      else if isRemoteComponent then
        (
          "besom.RemoteComponentResource".parse[Type].get,
          "besom.ComponentResourceOptions".parse[Type].get,
          "Component",
          "registerRemoteComponentResource"
        )
      else
        (
          "besom.CustomResource".parse[Type].get,
          "besom.CustomResourceOptions".parse[Type].get,
          "Custom",
          "readOrRegisterResource"
        )

    val baseClass =
      m"""|final case class $baseClassName private(
          |${baseClassParams.map(param => s"  ${param.syntax}").mkString(",\n")}
          |) extends ${resourceBaseClass}""".stripMargin.parse[Stat].get

    val (methodFiles, baseClassMethods) =
      methods.map {
        case (name, (functionCoordinates, (functionDefinition, false))) =>
          val functionToken     = functionCoordinates.token
          val methodCoordinates = functionCoordinates.asFunctionClass
          if (!methodCoordinates.definitionName.contains(name)) {
            logger.warn(
              s"""|Resource definition method name '${name}' (used) does not match the name
                  |in method definition '${methodCoordinates.definitionName.getOrElse("")}' (ignored)
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
        case (_, (_, (_, true))) =>
          throw GeneralCodegenException("Overlay files are not supported for methods, resource overlay was expected instead")
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

    val decoderInstanceName = if isRemoteComponent then "remoteComponentResourceDecoder" else "customResourceDecoder"

    val decoderInstance =
      m"""  given decoder(using besom.types.Context): besom.types.Decoder[$baseClassName] =
         |    besom.internal.Decoder.$decoderInstanceName[$baseClassName]
         |""".stripMargin

    val unionDecoders = unionDecoderGivens(resourceProperties)

    def argsDefaultMsg = if argsDefault.isEmpty then "" else "This resource has a default configuration."

    val baseCompanion =
      if (hasOutputExtensions) {
        m"""|object $baseClassName extends besom.ResourceCompanion[$baseClassName]:
            |  /** Resource constructor for $baseClassName. 
            |    * 
            |    * @param name [[besom.util.NonEmptyString]] The unique (stack-wise) name of the resource in Pulumi state (not on provider's side).
            |    *        NonEmptyString is inferred automatically from non-empty string literals, even when interpolated. If you encounter any
            |    *        issues with this, please try using `: NonEmptyString` type annotation. If you need to convert a dynamically generated
            |    *        string to NonEmptyString, use `NonEmptyString.apply` method - `NonEmptyString(str): Option[NonEmptyString]`.
            |    *
            |    * @param args [[${argsClassName}]] The configuration to use to create this resource. $argsDefaultMsg
            |    *
            |    * @param opts [[${resourceOptsClass}]] Resource options to use for this resource. 
            |    *        Defaults to empty options. If you need to set some options, use [[besom.opts]] function to create them, for example:
            |    *  
            |    *        {{{
            |    *        val res = $baseClassName(
            |    *          "my-resource",
            |    *          ${argsClassName}(...), // your args
            |    *          opts(provider = myProvider)
            |    *        )
            |    *        }}}
            |    */
            |  def apply(using ctx: besom.types.Context)(
            |    name: besom.util.NonEmptyString,
            |    args: ${argsClassName}${argsDefault},
            |    opts: besom.ResourceOptsVariant.$variant ?=> ${resourceOptsClass} = ${resourceOptsClass}()
            |  ): besom.types.Output[$baseClassName] =
            |    ctx.${resourceRegisterMethodName}[$baseClassName, $argsClassName](${tokenLit}, name, args, opts(using besom.ResourceOptsVariant.$variant))
            |
            |  private[besom] def typeToken: besom.types.ResourceType = ${tokenLit}
            |
            |$resourceDecoderInstance
            |$decoderInstance
            |${unionDecoders.mkString("\n")}
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

  def sourceFilesForFunctions(packageInfo: PulumiPackageInfo): Seq[SourceFile] = {
    given Config.Provider = packageInfo.providerConfig
    packageInfo.parsedFunctions
      .filterNot { case (_, (f, _)) => isMethod(f) }
      .map {
        case (coordinates, (functionDefinition, false)) =>
          sourceFilesForFunction(
            functionCoordinates = coordinates,
            functionDefinition = functionDefinition
          )
        case (coordinates, (_, true)) =>
          Overlay.readFiles(
            packageInfo,
            coordinates.token,
            Vector(
              coordinates.asFunctionClass,
              coordinates.asFunctionArgsClass,
              coordinates.asFunctionResultClass
            )
          )
      }
      .toSeq
      .flatten
  }

  private def functionSupport(
    functionCoordinates: PulumiDefinitionCoordinates,
    functionDefinition: FunctionDefinition
  )(using Config.Provider): (Seq[SourceFile], Type.Ref, String, Type) = {
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

    val argsClassCoordinates = functionCoordinates.asFunctionArgsClass
    val argsClassSourceFile = makeArgsClassSourceFile(
      classCoordinates = argsClassCoordinates,
      properties = inputProperties,
      isResource = false,
      isProvider = false
    )

    val resultClassCoordinates = functionCoordinates.asFunctionResultClass
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
        properties = outputProperties,
        requiresJsonFormat = false
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
  )(using Config.Provider): Seq[SourceFile] = {
    val (supportClassSourceFiles, argsClassRef, argsDefault, resultTypeRef) = functionSupport(
      functionCoordinates,
      functionDefinition
    )
    val functionToken     = functionCoordinates.token
    val methodCoordinates = functionCoordinates.asFunctionClass

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

  def sourceFilesForConfig(
    packageInfo: PulumiPackageInfo
  ): (Seq[SourceFile], Seq[ConfigDependency]) = {
    val PulumiToken(_, _, providerName) = PulumiToken(packageInfo.providerTypeToken)
    val coordinates = PulumiToken(providerName, Utils.configModuleName, Utils.configTypeName)
      .toCoordinates(packageInfo)
      .asConfigClass

    val configsWithDefault = packageInfo.pulumiPackage.config.defaults
    val configVariables    = packageInfo.pulumiPackage.config.variables.toSeq.sortBy(_._1)
    val configs = configVariables.collect { case (configName, configDefinition) =>
      val get      = Name(s"get${configName.capitalize}")
      val propType = configDefinition.typeReference.asScalaType()
      val isSecret = Lit.Boolean(configDefinition.secret)
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
        case Some(message) => s"\n@deprecated(\"\"\"$message\"\"\")"
        case None          => ""
      }
      val deprecationDocs = configDefinition.deprecationMessage match {
        case Some(message) => s"""\n* @deprecated $message"""
        case None          => ""
      }
      val returnsDoc = s"@returns the value of the `$providerName:$configName` configuration property" +
        s"${if configsWithDefault.contains(configName) then " or a default value if present" else ""}."
      val providerNameLit = Lit.String(providerName)
      val configNameLit   = Lit.String(configName)
      val code =
        m"""|/**
            | * ${description}${deprecationDocs}
            | * ${returnsDoc}
            | */${deprecationCode}
            |def ${get}(using ${scalameta.types.besom.types.Context}): ${scalameta.types.besom.types
             .Output(
               scalameta.types.Option(propType)
             )} =
            |  besom.internal.Codegen.config[${propType}](${providerNameLit})(key = ${configNameLit}, isSecret = ${isSecret}, default = ${default}, environment = ${environment})
            |""".stripMargin

      val _ = scalameta.parseStatement(code)
      code
    }

    val code =
      m"""|package ${coordinates.packageRef.syntax}
          |import ${scalameta.besom.internal.CodegenProtocol().syntax}.*
          |${configs.mkString("\n")}
          |""".stripMargin

    if packageInfo.pulumiPackage.config.variables.isEmpty
    then (Seq.empty, Seq.empty)
    else {
      val _ = scalameta.parseSource(code)

      val file    = FilePath(Seq("src", Utils.configModuleName, s"${Utils.configTypeName.toLowerCase}.scala"))
      val sources = Seq(SourceFile(file, code))
      val dependencies: Seq[ConfigDependency] = configVariables.flatMap { case (_, configDefinition) =>
        configDefinition.typeReference.asTokenAndDependency.flatMap {
          case (Some(token), None) => ConfigSourceDependency(token.toCoordinates(packageInfo)) :: Nil
          case (Some(token), Some(metadata)) =>
            ConfigRuntimeDependency(token.toCoordinates(packageInfo), metadata) :: Nil
          case _ => Nil
        }
      }

      (sources, dependencies)
    }
  }

  private def makeOutputClassSourceFile(
    classCoordinates: ScalaDefinitionCoordinates,
    properties: Seq[PropertyInfo],
    requiresJsonFormat: Boolean
  )(using Config.Provider): SourceFile = {
    val className = classCoordinates.definitionTypeName.getOrElse(
      throw GeneralCodegenException(
        s"Class name for ${classCoordinates.typeRef} could not be found"
      )
    )
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

    val jsonFormatExtensionMethod = Option.when(requiresJsonFormat) {
      scalameta.given_(scalameta.types.besom.json.JsonFormat(className)) {
        scalameta.besom.internal.CodegenProtocol.jsonFormatN
      }
    }

    val outputExtensionMethods = properties.map { propertyInfo =>
      val innerType =
        if propertyInfo.isOptional
        then scalameta.types.Option(propertyInfo.baseType)
        else propertyInfo.baseType

      // colon after underscore would be treated as a part of the name so we add a space
      m"""def ${propertyInfo.name} : besom.types.Output[$innerType] = output.map(_.${propertyInfo.name})"""
        .parse[Stat]
        .get
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

    // TODO: Add comments
    val classComment = ""

    val classDef =
      m"""|final case class ${className} private(
          |${classParams.map(arg => m"  ${arg.syntax}").mkString(",\n")}
          |)""".stripMargin
    val _ = scalameta.parseStatement(classDef)

    val decoderInstance =
      m"""|  given decoder(using besom.types.Context): besom.types.Decoder[$className] =
          |    besom.internal.Decoder.derived[$className]
          |""".stripMargin

    val unionDecoders = unionDecoderGivens(properties)

    val baseCompanionDef =
      if (hasOutputExtensions) {
        // colon after underscore would be treated as a part of the name so we add a space
        m"""|object ${className} :
            |${jsonFormatExtensionMethod.map("  " + _.syntax).getOrElse("")}
            |$decoderInstance
            |${unionDecoders.mkString("\n")}
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[$className])
            |${outputExtensionMethods.map(meth => m"      ${meth.syntax}").mkString("\n")}
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[$className]])
            |${optionOutputExtensionMethods.map(meth => m"      ${meth.syntax}").mkString("\n")}
            |""".stripMargin
      } else {
        m"""|object $className:
            |$decoderInstance
            |${unionDecoders.mkString("\n")}
            |""".stripMargin
      }
    val _ = scalameta.parseStatement(baseCompanionDef)

    val imports =
      if requiresJsonFormat then s"import ${scalameta.besom.internal.CodegenProtocol().syntax}.*" :: Nil
      else Nil

    val fileContent =
      m"""|package ${classCoordinates.packageRef}
          |${imports.mkString("\n")}
          |${classComment}
          |${classDef}
          |${baseCompanionDef}
          |""".stripMargin
    val _ = scalameta.parseSource(fileContent)

    SourceFile(
      classCoordinates.filePath,
      fileContent
    )
  }

  private def makeArgsClassSourceFile(
    classCoordinates: ScalaDefinitionCoordinates,
    properties: Seq[PropertyInfo],
    isResource: Boolean,
    isProvider: Boolean
  )(using Config.Provider): SourceFile = {
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
        m"""|  given providerArgsEncoder(using besom.types.Context): besom.types.ProviderArgsEncoder[$argsClassName] =
            |    besom.internal.ProviderArgsEncoder.derived[$argsClassName]
            |""".stripMargin

      lazy val argsEncoderInstance =
        m"""|  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[$argsClassName] =
            |    besom.internal.ArgsEncoder.derived[$argsClassName]
            |""".stripMargin

      if (isProvider)
        m"""|  given encoder(using besom.types.Context): besom.types.Encoder[$argsClassName] =
            |    besom.internal.Encoder.derived[$argsClassName]
            |$providerArgsEncoderInstance""".stripMargin
      else if (isResource)
        m"""|  given encoder(using besom.types.Context): besom.types.Encoder[$argsClassName] =
            |    besom.internal.Encoder.derived[$argsClassName]
            |$argsEncoderInstance""".stripMargin
      else
        m"""|  given encoder(using besom.types.Context): besom.types.Encoder[$argsClassName] =
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

  private def unionDecoderGivens(properties: Seq[PropertyInfo]): List[String] = {
    // make sure that the name is unique and we don't have collisions
    def decoderUniqueName(typ: Type) = "uDec" + sha256(typ.syntax)
    properties
      .collect {
        case p: PropertyInfo if p.unionMappings.nonEmpty =>
          p.unionMappings.map {
            case TypeMapper.UnionMapping.ByField(unionType, keyPropertyName, keyToType) =>
              val keyPropertyNameLit = Lit.String(keyPropertyName)
              val indexesLit = scalameta.Map(keyToType.map { case (key, typ) =>
                val keyLit: Term  = Lit.String(key)
                val decoder: Term = scalameta.summon_(scalameta.types.besom.internal.Decoder(typ))
                Term.Tuple(List(keyLit, decoder))
              }.toList)
              val name = decoderUniqueName(unionType)
              name -> m"""|  given ${name}(using besom.types.Context): besom.types.Decoder[$unionType] =
                          |    besom.internal.Decoder.discriminated($keyPropertyNameLit, $indexesLit)
                          |""".stripMargin
            case TypeMapper.UnionMapping.ByIndex(unionType, indexToType) =>
              val indexesLit = scalameta.Map(indexToType.map { case (index, typ) =>
                val keyLit: Term  = Lit.Int(index)
                val decoder: Term = scalameta.summon_(scalameta.types.besom.internal.Decoder(typ))
                Term.Tuple(List(keyLit, decoder))
              }.toList)
              val name = decoderUniqueName(unionType)
              name -> m"""|  given ${name}(using besom.types.Context): besom.types.Decoder[$unionType] =
                          |    besom.internal.Decoder.nonDiscriminated($indexesLit)
                          |""".stripMargin
          }
      }
      .map(_.toMap)
      .foldLeft(Map.empty[String, String])(_ ++ _)
      .values
      .toList // de-duplicate givens
  }
}

object CodeGen:
  def packageDependency(name: SchemaName, version: SchemaVersion)(using Config): String =
    packageDependencies(List((name, version)))
  def packageDependencies(dependencies: List[(SchemaName, SchemaVersion)])(using config: Config): String =
    dependencies
      .map { case (name, version) =>
        s"""|//> using dep "org.virtuslab::besom-${name}:${version}-core.${config.coreShortVersion}"
            |""".stripMargin
      }
      .mkString("\n")

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
