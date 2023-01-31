package besom.codegen

import scala.meta._
import scala.meta.dialects.Scala33

import java.nio.file.{Path, Paths}

// parsing json -> semantic transform -> code generation

// def genClasses(packageName: String, classes: Seq[GenClass]): String = ???

// def genClass(clazz: GenClass): Defn =
//   Seq(genClassDef(clazz), genCompanion(clazz) genFactoryMethod(clazz)) 


class CodeGen(/* basePackageParts: List[String] */ basePackage: String, packageMappings: Map[String, String]) {
  // def typeFilePath(spec: TypeSpec, prefix: String): Path = {
  //   //tpeSpec.
  //   val typeIdParts = spec.typeId.split(":")
  //   //val providerName = typeIdParts(0)
  //   val packageSuffix = packageMappings.getOrElse(key = typeIdParts(1), default = typeIdParts(1))
  //   val path = s""
  // }
  
  // def genOutputClass(spec: TypeSpec) = {
  //   val outputSpec = OutputSpec(

  //   )
  // }
  
  // def genOutputWithArgsClass(spec: TypeSpec) = {
  //   // Defn.Class(
  //   //   mods = List(Mod.Case()),
  //   //   name = Type.Name(classSpec.name),
  //   //   tparamClause = Type.ParamClause(List.empty),
  //   //   ctor = Ctor.Primary(

  //   //   )
  //   // )
  //   val className = Type.Name(spec.name)
  //   val fields: List[Term.Param] = spec.properties.map { fieldSpec =>
  //     Term.Param(
  //       mods = List.empty,
  //       name = Term.Name(fieldSpec.name),
  //       decltpe = Some(fieldSpec.tpe.asScalaType(basePackage, packageMappings)),
  //       default = fieldSpec.tpe.defaultValue
  //     )
  //   }.toList
  //   // val fields = List(
  //   //   q"""val x: Int"""
  //   // )
  //   val classParams = Term.ParamClause(fields)
  //   val outputClass = q"""case class ${className}(..${classParams})"""
  // }

  // def genResourceClass(spec: ClassSpec) = {

  // }

  // def genArgsClass(spec: ClassSpec) = {
  //   val className = Type.Name(spec.name)
  //   q"""case class ${className}(..)"""
  // }

  def decapitalize(s: String) = s(0).toLower.toString ++ s.substring(1, s.length)

  def genForResource(spec: TypeSpec): (String, Path) = {
    val typeIdParts = spec.typeId.split(":")
    val providerName = typeIdParts(0)
    val packageSuffix = packageMappings.getOrElse(key = typeIdParts(1), default = typeIdParts(1))
    val resourceName = typeIdParts(2)
    val resourceClassName = Type.Name(resourceName)
    val argsClassName = Type.Name(resourceClassName + "Args")
    val argsCompanionName = Term.Name(resourceClassName + "Args")
    val factoryMethodName = Term.Name(decapitalize(resourceName))

    val commonResourceProperties = List(
      FieldSpec(name = "urn", tpe = StringType, isRequired = true),
      FieldSpec(name = "id", tpe = StringType, isRequired = true)
    )

    val imports =
      """import besom.util.NotProvided
        |import besom.internal.Output
        |import besom.internal.CustomResourceOptions""".stripMargin

    val resourceProperties = commonResourceProperties ++ spec.properties

    val resourceClassArgs = resourceProperties.map { fieldSpec =>
      val fieldBaseType = fieldSpec.tpe.asScalaType(basePackage, packageMappings)
      val optionalFieldType = if (fieldSpec.isRequired) fieldBaseType else t"Option[$fieldBaseType]"
      val fieldType = t"Output[$optionalFieldType]"
      Term.Param(
        mods = List.empty,
        name = Term.Name(fieldSpec.name),
        decltpe = Some(fieldType),
        default = None
      )
    }

    val argsClassArgs = spec.inputProperties.get.map { fieldSpec =>
      val fieldBaseType = fieldSpec.tpe.asScalaType(basePackage, packageMappings)
      val fieldType = if (fieldSpec.isRequired) fieldBaseType else t"Option[$fieldBaseType]" 
      Term.Param(
        mods = List.empty,
        name = Term.Name(fieldSpec.name),
        decltpe = Some(fieldType),
        default = None
      )
    }

    val factoryMethod =
      s"""|def $factoryMethodName(using ctx: Context)(
          |  name: String,
          |  args: $argsClassName,
          |  opts: CustomResourceOptions = CustomResourceOptions()
          |): Output[$resourceClassName] = ???""".stripMargin

    val argsObjectApplyArgs = spec.inputProperties.get.map { fieldSpec =>
      val paramBaseType = fieldSpec.tpe.asScalaType(basePackage, packageMappings)
      val paramType =
        if (fieldSpec.tpe.isMapType)
          t"""Map[String, $paramBaseType] | Map[String, Output[$paramBaseType]] | Output[Map[String, $paramBaseType]] | NotProvided"""
        else
          t"""$paramBaseType | Output[$paramBaseType] | NotProvided"""

      Term.Param(
        mods = List.empty,
        name = Term.Name(fieldSpec.name),
        decltpe = Some(paramType),
        default = Some(q"NotProvided")
      )
    }

    val argsObjectApplyBodyArgs: List[Term] = spec.inputProperties.get.map { fieldSpec =>
      val fieldTermName = Term.Name(fieldSpec.name)
      val argValue = 
        if (fieldSpec.tpe.isMapType)
          q"${fieldTermName}.asMapOutput"
        else
          q"${fieldTermName}.asOutput"
      Term.Assign(fieldTermName, argValue)
    }

    val packageDecl = s"package ${basePackage}.${providerName}.${packageSuffix}"

    // TODO: Should we show entire descriptions as comments? Formatting of comments should be preserved
    // val resourceComment = spec.description.fold("")(desc => s"/**\n${desc}\n*/\n") // TODO: Escape/sanitize comments
    val resourceComment = ""

    val resourceClass =
      s"""|case class $resourceClassName(
          |${resourceClassArgs.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives ResourceDecoder""".stripMargin

    val argsClass =
      s"""|case class $argsClassName(
          |${argsClassArgs.map(arg => s"  ${arg}").mkString(",\n")}
          |) derives ArgsEncoder""".stripMargin

    val argsCompanion =
      s"""|object $argsCompanionName:
          |  def apply(
          |${argsObjectApplyArgs.map(arg => s"    ${arg}").mkString(",\n")}
          |  )(using Context): $argsClassName =
          |    new $argsClassName(
          |${argsObjectApplyBodyArgs.map(arg => s"      ${arg}").mkString(",\n")}
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

    val pathParts = basePackage.split("\\.") ++ Seq(providerName) ++ packageSuffix.split("\\.") ++ Seq(s"${resourceName}.scala")
    
    val filePath = Paths.get(pathParts.head, pathParts.tail.toArray: _*)

    (fileContent, filePath)
  }
}


object CodeGen {
  def genFromSchema(schemaFilePath: String, destRootPath: String) = {
    val packagePrefix = "besom.api"
    val packageMappings = SchemaReader.readPackageMappings(schemaFilePath)

    // val outputSpecs = SchemaReader.readOutputs(filePath)
    val codeGen = new CodeGen(
      packagePrefix,
      packageMappings
    )

    val resourceSpecs = SchemaReader.readResources(schemaFilePath)

    resourceSpecs
      .foreach{ res =>
        println("!!!!!!")
        println(res)
        val code = codeGen.genForResource(res)
        // val destFileSuffix = 
      }
  }
  // def fromFile(path: String, packagePrefix: String) = {
  //   val packageMappings = SchemaReader.readPackageMappings(path)
  //   val resourceSpecs = SchemaReader.readResources(path)
  //   val outputSpecs = SchemaReader.readOutputs(path)
  //   new CodeGen(
  //     packagePrefix/* .split("\\.").toList */,
  //     packageMappings
  //   )
  // }
  // def genResource(classSpec: ClassSpec)
  // def genInput(classSpec: ClassSpec)


  // def genOutput(classSpec: ClassSpec) = {
  //   // Defn.Class(
  //   //   mods = List(Mod.Case()),
  //   //   name = Type.Name(classSpec.name),
  //   //   tparamClause = Type.ParamClause(List.empty),
  //   //   ctor = Ctor.Primary(

  //   //   )
  //   // )
  //   val className = Type.Name(classSpec.name)
  //   val fields: List[Term.Param] = classSpec.fields.map { fieldSpec =>
  //     Term.Param(
  //       mods = List.empty,
  //       name = Term.Name(fieldSpec.name),
  //       decltpe = Some(fieldSpec.tpe.asScalaType),
  //       default = fieldSpec.tpe.defaultValue
  //     )
  //   }.toList
  //   // val fields = List(
  //   //   q"""val x: Int"""
  //   // )
  //   val classParams = Term.ParamClause(fields)
  //   q"""case class ${className}(..${classParams})"""
  // }
}

// def 





// object Test {
//   def main(args: Array[String]) = {
//     val objectMetadataCode = CodeGen.genOutput(Examples.ObjectMetadata)
    
//     println(objectMetadataCode)
//   }
// }

// object Examples {
//   val ObjectMetadata = ClassSpec("ObjectMetadata", Seq(
//     FieldSpec("annotations", MapType(StringType)),
//     FieldSpec("clusterName", UnionType(List(NullType, StringType))),
//     // FieldSpec("creationTimestamp", UnionType(List(NullType, StringType))),
//     // FieldSpec("deletionGracePeriodSeconds", UnionType(List(NullType, IntegerType))),
//     FieldSpec("finalizers", ArrayType(StringType)),
//     FieldSpec("managedFields", UnionType(List(NullType, NamedType("ManagedFieldsEntry")))),
//   ))
// }


//////

trait ManagedFieldsEntry

case class ObjectMeta(
    annotations: Map[String, String] = Map.empty,
    clusterName: Option[String],
    creationTimestamp: Option[String],
    deletionGracePeriodSeconds: Option[Int],
    deletionTimestamp: Option[String],
    finalizers: List[String] = List.empty,
    generateName: Option[String],
    generation: Option[Int],
    labels: Map[String, String] = Map.empty,
    managedFields: List[ManagedFieldsEntry] = List.empty,
    // name: Option[String],
    // namespace: Option[String],
    // ownerReferences: List[OwnerReference] = List.empty,
    // resourceVersion: Option[String],
    // selfLink: Option[String],
    // uid: Option[String]
  ) // derives ResourceOutputDecoder






  ///////



// package foo

// trait NotProvided

// case class Bar1(
// 	xxx: Int | NotProvided
// )

// object Bar1:
// 	def apply() = 1

// case class Bar2(
// 	xxx: String
// )