package besom.codegen

import besom.codegen.Config.{CodegenConfig, ProviderConfig}
import besom.codegen.PackageVersion.PackageVersion

import scala.meta._
import scala.meta.dialects.Scala33
import besom.codegen.metaschema._
import besom.codegen.Utils._

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
      s"""|//> using scala $scalaVersion
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

    val enumClassName       = Type.Name(classCoordinates.definitionName).syntax
    val enumClassStringName = Lit.String(classCoordinates.definitionName).syntax

    val (superclass, valueType) = enumDefinition.`type` match {
      case BooleanType => ("besom.types.BooleanEnum", "Boolean")
      case IntegerType => ("besom.types.IntegerEnum", "Int")
      case NumberType  => ("besom.types.NumberEnum", "Double")
      case StringType  => ("besom.types.StringEnum", "String")
    }

    val instances = enumDefinition.`enum`.map { valueDefinition =>
      val caseRawName = valueDefinition.name.getOrElse {
        valueDefinition.value match {
          case StringConstValue(value) => value
          case const                   => throw GeneralCodegenException(s"The name of enum cannot be derived from value ${const}")
        }
      }
      val caseName       = Term.Name(caseRawName).syntax
      val caseStringName = Lit.String(caseRawName).syntax
      val caseValue      = constValueAsCode(valueDefinition.value).syntax

      val definition = s"""object ${caseName} extends ${enumClassName}(${caseStringName}, ${caseValue})"""
      val reference  = caseName
      (definition, reference)
    }

    val fileContent =
      s"""|package ${classCoordinates.fullPackageName}
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
        makePropertyInfo(
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
    val baseClassName        = Type.Name(baseClassCoordinates.definitionName).syntax
    val argsClassName        = Type.Name(argsClassCoordinates.definitionName).syntax

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
        makePropertyInfo(
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
        makePropertyInfo(
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
        if (propertyInfo.isOptional) t"""scala.Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      Term
        .Param(
          mods = List.empty,
          name = propertyInfo.name,
          decltpe = Some(t"""besom.types.Output[${innerType}]"""),
          default = None
        )
        .syntax
    }

    val baseOutputExtensionMethods: Seq[String] = resourceProperties.map { propertyInfo =>
      val innerType =
        if (propertyInfo.isOptional) t"""scala.Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      val resultType = t"besom.types.Output[${innerType}]"
      q"""def ${propertyInfo.name}: $resultType = output.flatMap(_.${propertyInfo.name})""".syntax
    }

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val resourceBaseClass = if (isProvider) "besom.ProviderResource" else "besom.CustomResource"
    if (resourceDefinition.isComponent) {
      logger.warn(s"Component resources are not supported yet, generating incorrect resource ${token.asString}")
    }

    val baseClass =
      s"""|final case class $baseClassName private(
          |${baseClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives besom.ResourceDecoder""".stripMargin

    val (methodFiles, baseClassMethods) =
      methods.map { case (name, (functionCoordinates, functionDefinition)) =>
        val functionToken     = functionCoordinates.token
        val methodCoordinates = functionCoordinates.resourceMethod
        if (name != methodCoordinates.definitionName) {
          logger.warn(
            s"""|Resource definition method name '${name}' (used) does not match the name
                |in method definition '${methodCoordinates.definitionName}' (ignored)
                |for function '${functionToken.asString}' - this is a schema error""".stripMargin
          )
        }
        val (supportClassSourceFiles, argsClassRef, argsDefault, resultTypeRef) =
          functionSupport(functionCoordinates, functionDefinition)

        val thisTypeRef  = baseClassCoordinates.fullyQualifiedTypeRef
        val tokenLiteral = Lit.String(functionToken.asString)
        val code =
          s"""|  def ${Term.Name(name)}(using ctx: besom.types.Context)(
              |    args: ${argsClassRef}${argsDefault},
              |    opts: besom.InvokeOptions = besom.InvokeOptions()
              |  ): besom.types.Output[${resultTypeRef}] =
              |     ctx.call[$argsClassRef, $resultTypeRef, $thisTypeRef](${tokenLiteral}, args, this, opts)
              |""".stripMargin

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

    val baseCompanion =
      if (hasOutputExtensions) {
        s"""|object $baseClassName:
            |  def apply(using ctx: besom.types.Context)(
            |    name: besom.util.NonEmptyString,
            |    args: ${argsClassName}${argsDefault},
            |    opts: besom.CustomResourceOptions = besom.CustomResourceOptions()
            |  ): besom.types.Output[$baseClassName] =
            |    ctx.registerResource[$baseClassName, $argsClassName](${tokenLit}, name, args, opts)
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}""".stripMargin
      } else {
        s"""object $baseClassName"""
      }

    val baseClassFileContent =
      s"""|package ${baseClassCoordinates.fullPackageName}
          |${baseClassComment}
          |${baseClass}${if (baseClassMethods.nonEmpty) ":" else ""}
          |${baseClassMethods.mkString("\n")}
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
      functionDefinition.inputs.properties.toSeq.sortBy(_._1).collect { case (propertyName, propertyDefinition) =>
        makePropertyInfo(
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
          makePropertyInfo(
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

    val argsClassRef = argsClassCoordinates.fullyQualifiedTypeRef
    val argsDefault =
      if (inputProperties.isEmpty) {
        s" = ${argsClassRef}()"
      } else ""

    val resultTypeRef: Type =
      (functionDefinition.outputs.objectTypeDefinition, functionDefinition.outputs.typeReference) match {
        case (Some(_), _) =>
          resultClassCoordinates.fullyQualifiedTypeRef
        case (None, Some(ref)) =>
          ref.asScalaType()
        case (None, None) =>
          t"scala.Unit"
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

    val methodName = methodCoordinates.definitionName
    if (methodName.contains("/")) {
      throw GeneralCodegenException(s"Top level function name ${methodName} containing a '/' is not allowed")
    }
    val methodSourceFile = {
      val token = Lit.String(functionToken.asString)

      val fileContent =
        s"""|package ${methodCoordinates.fullPackageName}
            |
            |def ${Term.Name(methodName)}(using ctx: besom.types.Context)(
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

  private def makePropertyInfo(
    propertyName: String,
    propertyDefinition: PropertyDefinition,
    isPropertyRequired: Boolean
  ): PropertyInfo = {
    val isRequired =
      isPropertyRequired ||
        propertyDefinition.default.nonEmpty ||
        propertyDefinition.const.nonEmpty
    val baseType = propertyDefinition.typeReference.asScalaType()
    val argType  = propertyDefinition.typeReference.asScalaType(asArgsType = true)
    val inputArgType = propertyDefinition.typeReference match {
      case ArrayType(innerType) =>
        t"""scala.List[besom.types.Input[${innerType.asScalaType(asArgsType = true)}]]"""
      case MapType(innerType) =>
        t"""scala.Predef.Map[String, besom.types.Input[${innerType.asScalaType(asArgsType = true)}]]"""
      case tp =>
        tp.asScalaType(asArgsType = true)
    }
    val defaultValue =
      propertyDefinition.default
        .orElse(propertyDefinition.const)
        .map { value =>
          constValueAsCode(value)
        }
        .orElse {
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
      isSecret = propertyDefinition.secret
    )
  }

  private def makeOutputClassSourceFile(
    classCoordinates: ScalaDefinitionCoordinates,
    properties: Seq[PropertyInfo]
  ) = {
    val classParams = properties.map { propertyInfo =>
      val fieldType =
        if (propertyInfo.isOptional) t"""scala.Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      Term
        .Param(
          mods = List.empty,
          name = propertyInfo.name,
          decltpe = Some(fieldType),
          default = None
        )
        .syntax
    }
    val outputExtensionMethods = properties.map { propertyInfo =>
      val innerType =
        if (propertyInfo.isOptional) t"""scala.Option[${propertyInfo.baseType}]""" else propertyInfo.baseType
      q"""def ${propertyInfo.name}: besom.types.Output[$innerType] = output.map(_.${propertyInfo.name})""".syntax
    }
    val optionOutputExtensionMethods = properties.map { propertyInfo =>
      val innerMethodName = if (propertyInfo.isOptional) Term.Name("flatMap") else Term.Name("map")
      q"""def ${propertyInfo.name}: besom.types.Output[scala.Option[${propertyInfo.baseType}]] = output.map(_.${innerMethodName}(_.${propertyInfo.name}))""".syntax
    }

    val hasOutputExtensions = outputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val classComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val classComment = ""

    val className = classCoordinates.definitionName

    val classDef =
      s"""|final case class $className private(
          |${classParams.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives besom.types.Decoder""".stripMargin

    val baseCompanionDef =
      if (hasOutputExtensions) {
        val classNameTerminator =
          if (className.endsWith("_")) " " else "" // colon after underscore would be treated as a part of the name

        s"""|object ${className}${classNameTerminator}:
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[$className])
            |${outputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[$className]])
            |${optionOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}
            |""".stripMargin
      } else {
        s"""object $className"""
      }

    val fileContent =
      s"""|package ${classCoordinates.fullPackageName}
          |
          |${classComment}
          |${classDef}
          |
          |${baseCompanionDef}
          |
          |""".stripMargin

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
  ) = {
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

    val fileContent =
      s"""|package ${classCoordinates.fullPackageName}
          |
          |${argsClass}
          |
          |${argsCompanion}
          |""".stripMargin

    SourceFile(
      classCoordinates.filePath,
      fileContent
    )
  }

  private def makeArgsClass(
    argsClassName: String,
    inputProperties: Seq[PropertyInfo],
    isResource: Boolean,
    isProvider: Boolean
  ): String = {
    val derivedTypeclasses =
      if (isProvider) "besom.types.ProviderArgsEncoder"
      else if (isResource) "besom.types.ArgsEncoder"
      else "besom.types.Encoder, besom.types.ArgsEncoder"
    val argsClassParams = inputProperties.map { propertyInfo =>
      val fieldType =
        if (propertyInfo.isOptional) t"""scala.Option[${propertyInfo.argType}]""" else propertyInfo.argType
      Term
        .Param(
          mods = List.empty,
          name = propertyInfo.name,
          decltpe = Some(t"""besom.types.Output[${fieldType}]"""),
          default = None
        )
        .syntax
    }

    s"""|final case class $argsClassName private(
        |${argsClassParams.map(arg => s"  ${arg}").mkString(",\n")}
        |) derives ${derivedTypeclasses}""".stripMargin
  }

  private def makeArgsCompanion(argsClassName: String, inputProperties: Seq[PropertyInfo], isResource: Boolean) = {
    val argsCompanionApplyParams = inputProperties.filter(_.constValue.isEmpty).map { propertyInfo =>
      val paramType =
        if (propertyInfo.isOptional) t"""besom.types.Input.Optional[${propertyInfo.inputArgType}]"""
        else t"""besom.types.Input[${propertyInfo.inputArgType}]"""
      Term
        .Param(
          mods = List.empty,
          name = propertyInfo.name,
          decltpe = Some(paramType),
          default = propertyInfo.defaultValue
        )
        .syntax
    }

    val argsCompanionApplyBodyArgs = inputProperties.map { propertyInfo =>
      val isSecret = Lit.Boolean(propertyInfo.isSecret)
      val argValue = propertyInfo.constValue match {
        case Some(constValue) =>
          q"besom.types.Output(${constValue})"
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
        |  )(using besom.types.Context): $argsClassName =
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
    "toString"
  )

  // This logic must be undone the same way in codecs
  // Keep in sync with `unmanglePropertyName` in codecs.scala
  private def manglePropertyName(name: String)(implicit logger: Logger): String =
    if (anyRefMethodNames.contains(name)) {
      val mangledName = name + "_"
      logger.warn(s"Mangled property name '$name' as '$mangledName'")
      mangledName
    } else name
}

case class FilePath private (pathParts: Seq[String]) {
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
