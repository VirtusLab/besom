package besom.codegen

import java.nio.file.{Path, Paths}

import scala.meta._
import scala.meta.dialects.Scala33

import besom.codegen.metaschema._
import besom.codegen.Utils._

class CodeGen(implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger) {
  val commonImportedIdentifiers = Seq(
    //"besom.util.NotProvided",
    "besom.internal.Output",
    "besom.internal.Context"
  )

  def sourcesFromPulumiPackage(pulumiPackage: PulumiPackage, besomVersion: String): Seq[SourceFile] = {
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
        pluginVersion = pulumiPackage.version.getOrElse {
          val defaultVersion = "0.0.1-SNAPSHOT"
          logger.warn(s"Pulumi package version missing - defaulting to ${defaultVersion}")
          defaultVersion
        }
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

  def sourceFilesForObjectType(typeCoordinates: PulumiTypeCoordinates, objectTypeDefinition: ObjectTypeDefinition, typeToken: String)/* (implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger) */: Seq[SourceFile] = {
    val baseClassCoordinates = typeCoordinates.asObjectClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asObjectClass(asArgsType = true)
    
    val baseClassName = Type.Name(baseClassCoordinates.className).syntax
    val argsClassName = Type.Name(argsClassCoordinates.className).syntax
    
    val baseFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.internal.Decoder"
    ))

    val argsFileImports = makeImportStatements(commonImportedIdentifiers ++ Seq(
      "besom.internal.Input",
      "besom.internal.Encoder",
      "besom.internal.ArgsEncoder",
      "besom.internal.Optional"
    ))

    // val objectProperties = {
    //   val allProperties = objectTypeDefinition.properties.toSeq.sortBy(_._1)
    //   if (allProperties.size <= jvmMaxParamsCount)
    //     allProperties
    //   else {
    //     logger.warn(s"Object type ${typeToken} has too many properties. Only first ${jvmMaxParamsCount} will be kept")
    //     allProperties.take(jvmMaxParamsCount)
    //   }
    // }

    val objectProperties = objectTypeDefinition.properties.toSeq.sortBy(_._1).map {
      case (propertyName, propertyDefinition) => TypeProperty(
        name = propertyName,
        definition = propertyDefinition,
        objectTypeDefinition.required.contains(propertyName)
      )
    }

    val baseClassParams = objectProperties.map(makeNonResourceBaseClassParam)
    val baseOutputExtensionMethods = objectProperties.map(makeNonResourceBaseOutputExtensionMethod)
    val baseOptionOutputExtensionMethods = objectProperties.map(makeNonResourceBaseOptionOutputExtensionMethod)

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val baseClass =
      s"""|case class $baseClassName private(
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


    // val argsClass =
    //   s"""|case class $argsClassName(
    //       |${argsClassParams.map(param => s"  ${param}").mkString(",\n")}
    //       |) derives Encoder, ArgsEncoder""".stripMargin

    // val argsCompanion =
    //   s"""|object $argsClassName:
    //       |  def apply(
    //       |${argsCompanionApplyParams.map(param => s"    ${param}").mkString(",\n")}
    //       |  )(using Context): $argsClassName =
    //       |    new $argsClassName(
    //       |${argsCompanionApplyBodyArgs.map(arg => s"      ${arg}").mkString(",\n")}
    //       |    )""".stripMargin

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

  def sourceFilesForCustomResources(pulumiPackage: PulumiPackage)/* (implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger) */: Seq[SourceFile] = {
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

  def sourceFilesForResource(typeCoordinates: PulumiTypeCoordinates, resourceDefinition: ResourceDefinition, typeToken: String, isProvider: Boolean)/* (implicit providerConfig: Config.ProviderConfig, typeMapper: TypeMapper, logger: Logger) */: Seq[SourceFile] = {
    val baseClassCoordinates = typeCoordinates.asResourceClass(asArgsType = false)
    val argsClassCoordinates = typeCoordinates.asResourceClass(asArgsType = true)
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
      val baseIdentifiers = Seq(
        "besom.internal.Input",
        "besom.internal.Encoder",
        "besom.internal.Optional"
      )
      val conditionalIdentifiers =
        if (isProvider)
          Seq("besom.internal.ProviderArgsEncoder")
        else
          Seq("besom.internal.ArgsEncoder")
      makeImportStatements(commonImportedIdentifiers ++ baseIdentifiers ++ conditionalIdentifiers)
    }

    val resourceBaseProperties = Seq(
      "urn" -> PropertyDefinition(typeReference = UrnType),
      "id" -> PropertyDefinition(typeReference = ResourceIdType)
    )

    // val resourceProperties = {
    //   val allProperties = (resourceBaseProperties ++ resourceDefinition.properties.toSeq.sortBy(_._1))
    //   if (allProperties.size <= jvmMaxParamsCount)
    //     allProperties
    //   else {
    //     logger.warn(s"Resource type ${typeToken} has too many properties. Only first ${jvmMaxParamsCount} will be kept")
    //     allProperties.take(jvmMaxParamsCount)
    //   }
    // }

    val requiredOutputs = resourceDefinition.required ++ List("urn", "id")

    val resourceProperties = (resourceBaseProperties ++ resourceDefinition.properties.toSeq.sortBy(_._1)).map {
      case (propertyName, propertyDefinition) => TypeProperty(
        name = propertyName,
        definition = propertyDefinition,
        requiredOutputs.contains(propertyName)
      )
    }

    val requiredInputs = resourceDefinition.requiredInputs

    val baseClassParams = resourceProperties.map(makeResourceBaseClassParam)

    val baseOutputExtensionMethods = resourceProperties.map(makeResourceBaseOutputExtensionMethod)

    // val inputProperties = {
    //   val allProperties = resourceDefinition.inputProperties.toSeq.sortBy(_._1)
    //   if (allProperties.size <= jvmMaxParamsCount)
    //     allProperties
    //   else {
    //     logger.warn(s"Resource type ${typeToken} has too many input properties. Only first ${jvmMaxParamsCount} will be kept")
    //     allProperties.take(jvmMaxParamsCount)
    //   }
    // }

    val inputProperties = resourceDefinition.inputProperties.toSeq.sortBy(_._1).map {
      case (propertyName, propertyDefinition) => TypeProperty(
        name = propertyName,
        definition = propertyDefinition,
        isRequired = requiredInputs.contains(propertyName)
      )
    }

    val hasOutputExtensions = baseOutputExtensionMethods.nonEmpty

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved. Examples for other languages should probably be filtered out.
    // val baseClassComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val baseClassComment = ""

    val resourceBaseClass = if (isProvider) "ProviderResource" else "CustomResource"

    val baseClass =
      s"""|case class $baseClassName private(
          |${baseClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives ResourceDecoder""".stripMargin

    val baseCompanion =
      if (hasOutputExtensions) {
        s"""|object $baseClassName:
            |  given outputOps: {} with
            |    extension(output: Output[$baseClassName])
            |${baseOutputExtensionMethods.map(meth => s"      ${meth}").mkString("\n")}""".stripMargin
      } else {
        s"""object $baseClassName"""
      }

    // the type has to match pulumi's resource type schema, ie kubernetes:core/v1:Pod
    val typ = Lit.String(typeToken)

    val hasDefaultArgsConstructor = resourceDefinition.requiredInputs.forall { propertyName =>
      val propertyDefinition = resourceDefinition.inputProperties(propertyName)
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

  private def makeResourceBaseClassParam(property: TypeProperty) = {
    val fieldBaseType = property.definition.typeReference.asScalaType()
    val fieldType = if (property.isRequired || property.definition.const.nonEmpty) t"Output[$fieldBaseType]" else t"Output[Option[$fieldBaseType]]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(manglePropertyName(property.name)),
      decltpe = Some(fieldType),
      default = None
    ).syntax
  }

  private def makeNonResourceBaseClassParam(property: TypeProperty) = {
    val fieldBaseType = property.definition.typeReference.asScalaType()
    val fieldType = if (property.isRequired || property.definition.const.nonEmpty) fieldBaseType else t"Option[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(manglePropertyName(property.name)),
      decltpe = Some(fieldType),
      default = None
    ).syntax
  }

  private def makeResourceBaseOutputExtensionMethod(property: TypeProperty) = {
    val propertyTermName = Term.Name(manglePropertyName(property.name))
    val fieldBaseType = property.definition.typeReference.asScalaType()
    val resultType = if (property.isRequired || property.definition.const.nonEmpty) t"Output[$fieldBaseType]" else t"Output[Option[$fieldBaseType]]"
    q"""def ${propertyTermName}: $resultType = output.flatMap(_.${propertyTermName})""".syntax
  }

  private def makeNonResourceBaseOutputExtensionMethod(property: TypeProperty) = {
    val propertyTermName = Term.Name(manglePropertyName(property.name))
    val fieldBaseType = property.definition.typeReference.asScalaType()
    val resultType = if (property.isRequired || property.definition.const.nonEmpty) t"Output[$fieldBaseType]" else t"Output[Option[$fieldBaseType]]"
    q"""def ${propertyTermName}: $resultType = output.map(_.${propertyTermName})""".syntax
  }

  private def makeNonResourceBaseOptionOutputExtensionMethod(property: TypeProperty) = {
    val propertyTermName = Term.Name(manglePropertyName(property.name))
    val fieldBaseType = property.definition.typeReference.asScalaType()
    val resultType = t"Output[Option[$fieldBaseType]]"
    val innerMethodName = if (property.isRequired || property.definition.const.nonEmpty) Term.Name("map") else Term.Name("flatMap")
    q"""def ${propertyTermName}: $resultType = output.map(_.${innerMethodName}(_.${propertyTermName}))""".syntax
  }

  private def makeArgsClassMember(property: TypeProperty) = {
    val memberName = Term.Name(manglePropertyName(property.name))
    val fieldBaseType = property.definition.typeReference.asScalaType(asArgsType = true)
    val fieldType = if (property.isRequired || property.definition.const.nonEmpty) t"Output[$fieldBaseType]" else t"Output[Option[$fieldBaseType]]"
    // val typedMember = Pat.Var(memberName), fieldType
    // q"""private var ${typedMember} = compiletime.uninitialized""".syntax

    Defn.Var(
      mods = List.empty,
      pats = List(Pat.Var(memberName)),
      decltpe = Some(fieldType),
      body = q"""compiletime.uninitialized"""
    ).syntax
  }

  private def makeArgsClassMemberRef(propertyName: String)(implicit logger: Logger)= {
    val memberName = Term.Name(manglePropertyName(propertyName))
    memberName.syntax
  }

  private def makeArgsClassMemberAssignment(property: TypeProperty) = {
    val memberName = Term.Name(manglePropertyName(property.name))
    val isSecret = Lit.Boolean(property.definition.secret)
    val memberValue = property.definition.const
      .map { const => q"""Output(${constValueAsCode(const)})""" }
      .getOrElse {
        val baseValue = q"""${memberName}.asOutput(isSecret = ${isSecret})"""
        if (property.isRequired || property.definition.const.nonEmpty) baseValue else q"""${baseValue}.map(_.asOption)"""
      }
    q"""builder_.${memberName} = ${memberValue}""".syntax
  }

  private def makeArgsClass(argsClassName: String, inputProperties: Seq[TypeProperty], isResource: Boolean, isProvider: Boolean) = {
    val argsEncoderClassName = if (isProvider) "ProviderArgsEncoder" else "ArgsEncoder"

    val argsClassMembers = inputProperties.map(makeArgsClassMember)

    val argsClassMembersRefs = inputProperties.map { property =>
      makeArgsClassMemberRef(propertyName = property.name)
    }

    s"""|final class $argsClassName private() extends besom.internal.ProductLike derives Encoder, ${argsEncoderClassName}:
        |${argsClassMembers.map(arg => s"  ${arg}").mkString("\n")}
        |
        |  override def productElems: List[Any] = List(${argsClassMembersRefs.mkString(", ")})
        |""".stripMargin
  }

  private def makeArgsCompanionApplyParam(property: TypeProperty) = {
    // val requiredParamType = property.definition.typeReference match {
    //   case MapType(additionalProperties) =>
    //     val valueType = additionalProperties.asScalaType(asArgsType = true)
    //     t"""scala.Predef.Map[String, $valueType] | scala.Predef.Map[String, Output[$valueType]] | Output[scala.Predef.Map[String, $valueType]]"""
    //   case tpe =>
    //     val baseType = tpe.asScalaType(asArgsType = true)
    //     t"""$baseType | Output[$baseType]"""
    // }

    // val paramType =
    //   if (isRequired) requiredParamType
    //   else t"""$requiredParamType | Option[$requiredParamType]"""

    // val innerParamType = property.definition.typeReference match {
    //   case MapType(additionalProperties) =>
    //     val valueType = additionalProperties.asScalaType(asArgsType = true)
    //     t"""scala.Predef.Map[String, Input[$valueType]]"""
      
    //   case tpe =>
    //     tpe.asScalaType(asArgsType = true)
    // }

    val innerParamType = property.definition.typeReference.asScalaType(asArgsType = true)

    val paramType =
      if (property.isRequired || property.definition.const.nonEmpty)
        t"""Input[$innerParamType]"""
      else
        t"""Input[Optional[$innerParamType]]"""

    // val optionalParamType = property.definition.typeReference match {
    //   case MapType(additionalProperties) =>
    //     val valueType = additionalProperties.asScalaType(asArgsType = true)
    //     t"""scala.Predef.Map[String, $valueType] | scala.Predef.Map[String, Output[$valueType]] | Output[scala.Predef.Map[String, $valueType]]"""
      
    //   case tpe =>
    //     val baseType = tpe.asScalaType(asArgsType = true)
    //     t"""$baseType | Output[$baseType]"""
    // }

    val default = property.definition.default match {
      case Some(defaultValue) =>
        Some(constValueAsCode(defaultValue))
      case None =>
        if (property.isRequired) None else Some(q"None")
    }

    Term.Param(
      mods = List.empty,
      name = Term.Name(manglePropertyName(property.name)),
      decltpe = Some(paramType),
      default = default
    ).syntax
  }

  // private def makeArgsCompanionApplyBodyArg(propertyName: String, property: PropertyDefinition) = {
  //   val fieldTermName = Term.Name(manglePropertyName(propertyName))
  //   val isSecret = Lit.Boolean(property.secret)
  //   val argValue = property.definition.const match {
  //     case Some(constValue) =>
  //       q"Output(${constValueAsCode(constValue)})"
  //     case None =>
  //       property.definition.typeReference match {
  //         case MapType(_) =>
  //           q"${fieldTermName}.asOutputMap(isSecret = ${isSecret})"
  //         case _ =>
  //           q"${fieldTermName}.asOutput(isSecret = ${isSecret})"
  //       }
  //   }
  //   Term.Assign(fieldTermName, argValue).syntax
  // }

  private def makeTupleType(types: List[Type]): Type = types match {
    case Nil => t"EmptyTuple"
    case tpe :: Nil => t"Tuple1[${tpe}]"
    case tpes => Type.Tuple(tpes)
  }

  private def makeMirroredElemLabelsTupleType(properties: Seq[TypeProperty])(implicit logger: Logger) = {
    val elemNameTypes = properties.map{ property => Lit.String(manglePropertyName(property.name)) }.toList
    val tupleType = elemNameTypes match {
      case Nil => t"EmptyTuple"
      case tpe :: Nil => t"Tuple1[${tpe}]"
      case tpes =>  Type.Tuple(tpes)
    }
    tupleType.syntax
  }

  private def makeProductLikeMirrorInstance(className: String, properties: Seq[TypeProperty]) = {
    val mirroredElemLabels = {
      val elemNameTypes = properties.map { property =>
        Lit.String(manglePropertyName(property.name))
      }.toList
      makeTupleType(elemNameTypes).syntax
    }

    val mirroredElemTypes = {
      val elemTypes = properties.map { property =>
        val fieldBaseType = property.definition.typeReference.asScalaType(asArgsType = true)
        if (property.isRequired || property.definition.const.nonEmpty) t"Output[$fieldBaseType]" else t"Output[Option[$fieldBaseType]]"
      }.toList
      makeTupleType(elemTypes).syntax
    }


    s"""|  given besom.internal.ProductLikeMirror[$className] with
        |    type MirroredElemLabels = ${mirroredElemLabels}
        |    type MirroredElemTypes = ${mirroredElemTypes}""".stripMargin
  }

  private def makeArgsCompanion(argsClassName: String, inputProperties: Seq[TypeProperty], isResource: Boolean) = {
    val argsCompanionApplyParams = inputProperties.filter(_.definition.const.isEmpty).map(makeArgsCompanionApplyParam)
    val argsClassMembersAssignments = inputProperties.map(makeArgsClassMemberAssignment)

    // val productLikeMirrorInstance = makeProductLikeMirrorInstance(className = )


    // // val argsCompanionApplyBodyArgs = inputProperties.map { case (propertyName, propertyDefinition) =>
    // //   makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition)
    // // }

    // val mirroredElemLabels = { //makeMirroredElemLabelsTupleType(inputProperties)
    //   val elemNameTypes = inputProperties.map { case (propertyName, _) =>
    //     Lit.String(manglePropertyName(propertyName))
    //   }.toList
    //   makeTupleType(elemNameTypes).syntax
    // }

    // val mirroredElemTypes = { // makeMirroredElemTypesTupleType(inputProperties, isRequiredProperty)
    //   val elemTypes = inputProperties.map { case (propertyName, propertyDefinition) =>
    //     val fieldBaseType = propertyDefinition.typeReference.asScalaType(asArgsType = true)
    //     val isRequired = isRequiredProperty(propertyName)
    //     if (isRequired || propertyDefinition.const.nonEmpty) t"Output[$fieldBaseType]" else t"Output[Option[$fieldBaseType]]"
    //   }.toList
    //   makeTupleType(elemTypes).syntax
    // }

    val productLikeMirrorInstance = makeProductLikeMirrorInstance(className = argsClassName, properties = inputProperties)

    s"""|object $argsClassName:
        |  private def constructor(): $argsClassName = new $argsClassName()
        |
        |  inline def apply(
        |${argsCompanionApplyParams.map(arg => s"    ${arg}").mkString(",\n")}
        |  )(using Context): $argsClassName =
        |    val builder_ = constructor()
        |${argsClassMembersAssignments.map(assignment => s"    ${assignment}").mkString("\n")}
        |    builder_
        |
        |${productLikeMirrorInstance}
        |""".stripMargin
  }


  // private def makeMirroredElemTypesTupleType

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

  // private val jvmMaxParamsCount = 253 // https://github.com/scala/bug/issues/7324 // TODO: Find some workaround to enable passing the remaining arguments

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

case class FilePath(pathParts: Seq[String]) {
  def osSubPath: os.SubPath = pathParts.foldLeft(os.sub)(_ / _)
}

case class SourceFile(
  filePath: FilePath,
  sourceCode: String
)
