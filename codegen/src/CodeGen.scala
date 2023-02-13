package besom.codegen

import java.nio.file.{Path, Paths}

import scala.meta._
import scala.meta.dialects.Scala33

object CodeGen {
  val basePackage = "besom.api"

  val commonImportedIdentifiers = Seq(
    "besom.util.NotProvided",
    "besom.internal.Output",
    "besom.types.PulumiArchive",
    "besom.types.PulumiAsset",
    "besom.types.PulumiAny",
    "besom.types.PulumiJson"
  )

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(basePackage: String, specialPackageMappings: Map[String, String]): Type = typeRef match {
      case BooleanType => t"Boolean"
      case StringType => t"String"
      case IntegerType => t"Int"
      case NumberType => t"Double"
      case ArrayType(elemType) => t"List[${elemType.asScalaType(basePackage, specialPackageMappings)}]"
      case MapType(elemType) => t"Map[String, ${elemType.asScalaType(basePackage, specialPackageMappings)}]"
      case unionType: UnionType =>
        unionType.oneOf.map(_.asScalaType(basePackage, specialPackageMappings)).reduce{ (t1, t2) => t"$t1 | $t2"}
      case namedType: NamedType =>
        //TODO: encoding of spaces in URIs
        namedType.ref match {
          // TODO: handle pulumi types:
          case "pulumi.json#/Archive" =>
            t"besom.types.PulumiArchive"
          case "pulumi.json#/Asset" =>
            t"besom.types.PulumiAsset"
          case "pulumi.json#/Any" =>
            t"besom.types.PulumiAny"
          case "pulumi.json#/Json" =>
            t"besom.types.PulumiJson"

          case escapedTypeUri =>
            // Example URI:
            // #/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject

            val typeIdParts = escapedTypeUri.split("#")(1).split(":")

            val providerName = typeIdParts(0).split("/").last
            val versionedPackage = typeIdParts(1).replace("%2F", "/") // TODO: Proper URL unescaping
            val packageSuffix = specialPackageMappings.getOrElse(key = versionedPackage, default = versionedPackage)
            val typeName = typeIdParts(2)

            val packageParts = basePackage.split("\\.") ++ Seq(providerName) ++ packageSuffix.split("\\.")
            val packageRef = packageParts.toList.foldLeft[Term.Ref](q"__root__")((acc, name) => Term.Select(acc, Term.Name(name)))

            t"${packageRef}.${Type.Name(typeName)}"

        }
      }

    def defaultValue: Option[Term] = typeRef match {
      case ArrayType(_) => Some(q"List.empty")
      case MapType(_) => Some(q"Map.empty")
      case tpe =>
        // TODO: default for options?
        None
    }

    def mapValueType = typeRef match {
      case MapType(additionalProperties) => Some(additionalProperties)
      case _ => None
    }
  }

  def sourcesFromPulumiPackage(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    Seq(
      sourceFileForBuildDefinition(providerName = pulumiPackage.name),
      sourceFileFromPulumiProvider(pulumiPackage)
    ) ++
    sourceFilesForPulumiTypes(pulumiPackage) ++
    sourceFilesForCustomResources(pulumiPackage)
  }

  def sourceFileForBuildDefinition(providerName: String): SourceFile = {
    // TODO reliable dependency on core module
    val fileContent =
      s"""|//> using scala "3.2.2"
          |//> using file "../../../../src"
          |""".stripMargin

    val pathParts = basePackage.split("\\.") ++ Seq(providerName, "project.scala")
    val filePath = Paths.get(pathParts.head, pathParts.tail.toArray: _*)

    SourceFile(relativePath = filePath, sourceCode = fileContent)
  }

  def sourceFileFromPulumiProvider(pulumiPackage: PulumiPackage): SourceFile = {
    val providerName = pulumiPackage.name
    val pathPrefixParts = basePackage.split("\\.") ++ Seq(providerName)
    val filePathPrefix = Paths.get(pathPrefixParts.head, pathPrefixParts.tail.toArray: _*)
    sourceFileForResource(
      resourceName = "Provider",
      resourceDefinition = pulumiPackage.provider,
      fullPackageName = s"${basePackage}.${providerName}",
      filePathPrefix = filePathPrefix,
      isProvider = false,
      packageMappings = pulumiPackage.language.java.packages
    )
  }

  def sourceFilesForPulumiTypes(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    val packageMappings = pulumiPackage.language.java.packages
    pulumiPackage.types.map { case (typeRef, typeDefinition) =>
      val typeIdParts = typeRef.split(":")
      val providerName = pulumiPackage.name
      val packageSuffix = packageMappings.getOrElse(key = typeIdParts(1), default = typeIdParts(1))
      val typeName = typeIdParts(2)
      val pathParts = basePackage.split("\\.") ++ Seq(providerName) ++ packageSuffix.split("\\.") ++ Seq(s"${typeName}.scala")

      val filePath = Paths.get(pathParts.head, pathParts.tail.toArray: _*)

      val packageDecl = s"package ${basePackage}.${providerName}.${packageSuffix}"
      val specificFileContent = typeDefinition match {
        case enumDef: EnumTypeDefinition => sourceForEnum(enumName = typeName, enumDefinition = enumDef)
        case objectDef: ObjectTypeDefinition => sourceForObjectType(typeName = typeName, objectTypeDefinition = objectDef, packageMappings = packageMappings)
      }
      val fileContent = packageDecl + "\n\n" + specificFileContent

      SourceFile(relativePath = filePath, sourceCode = fileContent)
    }.toSeq
  }

  def sourceForEnum(enumName: String, enumDefinition: EnumTypeDefinition): String = {
    val importedIdentifiers = commonImportedIdentifiers ++ Seq(
      "besom.internal.PulumiEnum"
    )
    val imports = importedIdentifiers.map(id => s"import $id").mkString("\n")

    val enumCases = enumDefinition.`enum`.map { valueDefinition =>
      val caseName = valueDefinition.name.getOrElse(valueDefinition.value)
      s"case ${caseName}" // TODO properly escape names
    } 
    
    val fileContent =
      s"""|${imports}
          |
          |enum ${enumName}:
          |${enumCases.map(arg => s"${arg}").mkString("\n")}
          |""".stripMargin

    fileContent
  }

  def sourceForObjectType(typeName: String, objectTypeDefinition: ObjectTypeDefinition, packageMappings: Map[String, String]): String = {
    val importedIdentifiers = commonImportedIdentifiers ++ Seq(
      "besom.internal.Decoder",
      "besom.internal.ArgsEncoder"
    )
    val imports = importedIdentifiers.map(id => s"import $id").mkString("\n")

    val outputName = typeName
    val outputClassName = Type.Name(outputName)
    val argsClassName = Type.Name(s"${outputClassName}Args")

    val outputClassArgs = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      val fieldBaseType = propertyDefinition.typeReference.asScalaType(basePackage, packageMappings)
      val isRequired = objectTypeDefinition.required.contains(propertyName)
      val fieldType = if (isRequired) fieldBaseType else t"Option[$fieldBaseType]"
      Term.Param(
        mods = List.empty,
        name = Term.Name(propertyName),
        decltpe = Some(fieldType),
        default = None
      )
    }

    val argsClassParams = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      makeArgsClassParam(propertyName = propertyName, property = propertyDefinition, packageMappings = packageMappings)
    }

    val argsCompanionApplyParams = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition, packageMappings = packageMappings)
    }

    val argsCompanionApplyBodyArgs = objectTypeDefinition.properties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition, packageMappings = packageMappings)
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
          |) derives ArgsEncoder""".stripMargin

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

  def sourceFilesForCustomResources(pulumiPackage: PulumiPackage): Seq[SourceFile] = {
    pulumiPackage.resources.map { case (resourceRef, resourceDefinition) =>
      val typeIdParts = resourceRef.split(":")
      // val providerName = typeIdParts(0)
      val providerName = pulumiPackage.name
      val packageMappings = pulumiPackage.language.java.packages
      val packageSuffix = packageMappings.getOrElse(key = typeIdParts(1), default = typeIdParts(1))
      val fullPackageName = s"${basePackage}.${providerName}.${packageSuffix}"
      val pathPrefixParts = basePackage.split("\\.") ++ Seq(providerName) ++ packageSuffix.split("\\.")
      // val pathParts = filePathPrefixParts ++ Seq(s"${resourceName}.scala")
    
      val filePathPrefix = Paths.get(pathPrefixParts.head, pathPrefixParts.tail.toArray: _*)
      
      sourceFileForResource(
        resourceName = resourceRef,
        resourceDefinition = resourceDefinition,
        fullPackageName = fullPackageName,
        filePathPrefix = filePathPrefix,
        isProvider = false,
        packageMappings = packageMappings,
      )
    }.toSeq
  }

  def sourceFileForResource(resourceName: String, resourceDefinition: ResourceDefinition, fullPackageName: String, filePathPrefix: Path, isProvider: Boolean, packageMappings: Map[String, String]): SourceFile = {
    val resourceClassName = Type.Name(resourceName)
    val argsClassName = Type.Name(s"${resourceClassName}Args")
    val factoryMethodName = Term.Name(decapitalize(resourceName))

    val importedIdentifiers = commonImportedIdentifiers ++ Seq(
      "besom.internal.CustomResourceOptions",
      "besom.internal.ResourceDecoder",
      "besom.internal.ArgsEncoder",
      "besom.internal.JsonEncoder",
    )

    val imports = importedIdentifiers.map(id => s"import $id").mkString("\n")

    val resourceBaseProperties = Seq(
      "urn" -> PropertyDefinition(typeReference = StringType),
      "id" -> PropertyDefinition(typeReference = StringType)
    )

    val resourceProperties = resourceBaseProperties ++ resourceDefinition.properties

    val resourceClassParams = resourceProperties.map { case (propertyName, propertyDefinition) =>
      makeResourceClassParam(propertyName = propertyName, property = propertyDefinition, packageMappings = packageMappings)
    }

    val argsClassParams = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsClassParam(propertyName = propertyName, property = propertyDefinition, packageMappings = packageMappings)
    }

    val argsCompanionApplyParams = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyParam(propertyName = propertyName, property = propertyDefinition, packageMappings = packageMappings)
    }

    val argsCompanionApplyBodyArgs = resourceDefinition.inputProperties.map { case (propertyName, propertyDefinition) =>
      makeArgsCompanionApplyBodyArg(propertyName = propertyName, property = propertyDefinition, packageMappings = packageMappings)
    }

    val packageDecl = s"package ${fullPackageName}"

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved
    // val resourceComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val resourceComment = ""

    val resourceBaseClass = if (isProvider) "besom.internal.ProviderResource" else "besom.internal.CustomResource"

    val resourceClass =
      s"""|case class $resourceClassName(
          |${resourceClassParams.map(param => s"  ${param}").mkString(",\n")}
          |) extends ${resourceBaseClass} derives ResourceDecoder""".stripMargin

    val factoryMethod =
      s"""|def $factoryMethodName(using ctx: Context)(
          |  name: String,
          |  args: $argsClassName,
          |  opts: besom.internal.CustomResourceOptions = besom.internal.CustomResourceOptions()
          |): Output[$resourceClassName] = ???""".stripMargin

    val argsEncoderClassName = if (isProvider) "besom.internal.ProviderArgsEncoder" else "besom.internal.ArgsEncoder"

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

  private def makeResourceClassParam(propertyName: String, property: PropertyDefinition, packageMappings: Map[String, String]) = {
    val fieldBaseType = property.typeReference.asScalaType(basePackage, packageMappings)
    val fieldType = t"Output[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(propertyName),
      decltpe = Some(fieldType),
      default = None
    )
  }

  private def makeArgsClassParam(propertyName: String, property: PropertyDefinition, packageMappings: Map[String, String]): Term.Param = {
    val fieldBaseType = property.typeReference.asScalaType(basePackage, packageMappings)
    val fieldType = t"Output[$fieldBaseType]"
    Term.Param(
      mods = List.empty,
      name = Term.Name(propertyName),
      decltpe = Some(fieldType),
      default = None
    )
  }

  private def makeArgsCompanionApplyParam(propertyName: String, property: PropertyDefinition, packageMappings: Map[String, String]): Term.Param = {
    val paramType = property.typeReference match {
      case MapType(additionalProperties) =>
        val valueType = additionalProperties.asScalaType(basePackage, packageMappings)
        t"""Map[String, $valueType] | Map[String, Output[$valueType]] | Output[Map[String, $valueType]] | NotProvided"""
      case tpe =>
        val baseType = tpe.asScalaType(basePackage, packageMappings)
        t"""$baseType | Output[$baseType] | NotProvided"""
    }

    Term.Param(
      mods = List.empty,
      name = Term.Name(propertyName),
      decltpe = Some(paramType),
      default = Some(q"NotProvided")
    )
  }

  private def makeArgsCompanionApplyBodyArg(propertyName: String, property: PropertyDefinition, packageMappings: Map[String, String]): Term.Assign = {
    val fieldTermName = Term.Name(propertyName)
    val isSecret = Lit.Boolean(property.secret)
    val argValue = property.typeReference match {
      case MapType(_) =>
        q"${fieldTermName}.asMapOutput(isSecret = ${isSecret})"
      case tpe =>
        q"${fieldTermName}.asOutput(isSecret = ${isSecret})"
    }
    Term.Assign(fieldTermName, argValue)
  }

  private def decapitalize(s: String) = s(0).toLower.toString ++ s.substring(1, s.length)
}

case class SourceFile(
  relativePath: Path,
  sourceCode: String
)
