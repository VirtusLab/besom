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
      typeToken = PulumiToken(pulumiPackage.providerTypeToken),
      isProvider = true
    )
  }

  def sourceFilesForNonResourceTypes(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    val moduleToPackageParts   = pulumiPackage.moduleToPackageParts
    val providerToPackageParts = pulumiPackage.providerToPackageParts

    pulumiPackage.types.flatMap { case (typeToken, typeDefinition) =>
      val typeCoordinates =
        PulumiDefinitionCoordinates.fromRawToken(typeToken, moduleToPackageParts, providerToPackageParts)

      typeDefinition match {
        case enumDef: EnumTypeDefinition =>
          sourceFilesForEnum(typeCoordinates = typeCoordinates, enumDefinition = enumDef)
        case objectDef: ObjectTypeDefinition =>
          sourceFilesForObjectType(
            typeCoordinates = typeCoordinates,
            objectTypeDefinition = objectDef,
            typeToken = PulumiToken(typeToken)
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

    val instances = enumDefinition.`enum`.map { valueDefinition =>
      val caseRawName = valueDefinition.name.getOrElse {
        valueDefinition.value match {
          case StringConstValue(value) => value
          case const => throw GeneralCodegenException(s"The name of enum cannot be derived from value ${const}")
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
    objectTypeDefinition: ObjectTypeDefinition,
    typeToken: PulumiToken
  ): Seq[SourceFile] = {
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
    val moduleToPackageParts   = pulumiPackage.moduleToPackageParts
    val providerToPackageParts = pulumiPackage.providerToPackageParts

    pulumiPackage.resources
      .collect {
        case (typeToken, resourceDefinition) if !resourceDefinition.isOverlay =>
          sourceFilesForResource(
            typeCoordinates =
              PulumiDefinitionCoordinates.fromRawToken(typeToken, moduleToPackageParts, providerToPackageParts),
            resourceDefinition = resourceDefinition,
            typeToken = PulumiToken(typeToken),
            isProvider = false
          )
      }
      .toSeq
      .flatten
  }

  def sourceFilesForResource(
    typeCoordinates: PulumiDefinitionCoordinates,
    resourceDefinition: ResourceDefinition,
    typeToken: PulumiToken,
    isProvider: Boolean
  ): Seq[SourceFile] = {
    val baseClassCoordinates = typeCoordinates.asResourceClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asResourceClass(asArgsType = true)
    val baseClassName        = Type.Name(baseClassCoordinates.definitionName).syntax
    val argsClassName        = Type.Name(argsClassCoordinates.definitionName).syntax

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
            s"Resource type ${typeToken.asString} has too many input properties. Only first ${jvmMaxParamsCount} will be kept"
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

    val baseOutputExtensionMethods = resourceProperties.map { propertyInfo =>
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
            |${baseOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}""".stripMargin
      } else {
        s"""object $baseClassName"""
      }

    val baseClassFileContent =
      s"""|package ${baseClassCoordinates.fullPackageName}
          |import besom.internal.Encoder.*
          |import besom.internal.Decoder.*
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
            functionCoordinates =
              PulumiDefinitionCoordinates.fromRawToken(functionToken, moduleToPackageParts, providerToPackageParts),
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
        throw GeneralCodegenException(s"Top level function name ${methodName} containing a '/' is not allowed")
      }

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

      Seq(argsClassSourceFile, methodSourceFile) ++ resultClassSourceFileOpt
    }
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
          |import besom.internal.Encoder.*
          |import besom.internal.Decoder.*
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
          |import besom.internal.Encoder.*
          |import besom.internal.Decoder.*
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
