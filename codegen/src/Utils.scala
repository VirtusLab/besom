package besom.codegen

import besom.codegen.metaschema.*

import scala.meta.{Lit, Type}

object Utils {
  // "index" is a placeholder module for classes that should be in
  // the root package (according to pulumi's convention)
  // Needs to be used in Pulumi types, but should NOT be translated to Scala code
  // Placeholder module for classes that should be in the root package (according to pulumi's convention)
  val indexModuleName  = "index"
  val configModuleName = "config"
  val providerTypeName = "Provider"
  val configTypeName   = "Config"

  // Name of the self parameter of resource methods
  val selfParameterName = "__self__"

  // TODO: Find some workaround to enable passing the remaining arguments
  val jvmMaxParamsCount = 253 // https://github.com/scala/bug/issues/7324

  type FunctionName  = String
  type FunctionToken = String

  implicit class ConstValueOps(constValue: ConstValue) {
    def asScala: Lit = constValue match {
      case StringConstValue(value)  => Lit.String(value)
      case BooleanConstValue(value) => Lit.Boolean(value)
      case IntConstValue(value)     => Lit.Int(value)
      case DoubleConstValue(value)  => Lit.Double(value)
    }
  }

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asTokenAndDependency(using typeMapper: TypeMapper): Vector[(Option[PulumiToken], Option[PackageMetadata])] =
      typeMapper.findTokenAndDependencies(typeRef)

    def asScalaType(asArgsType: Boolean = false)(using typeMapper: TypeMapper): Type =
      try {
        typeMapper.asScalaType(typeRef, asArgsType)
      } catch {
        case t: Throwable =>
          throw TypeMapperError(s"Failed to map type: '${typeRef}', asArgsType: $asArgsType", t)
      }
  }

  implicit class PulumiPackageOps(pulumiPackage: PulumiPackage) {
    def toPackageMetadata(overrideMetadata: PackageMetadata): PackageMetadata =
      toPackageMetadata(Some(overrideMetadata))
    def toPackageMetadata(overrideMetadata: Option[PackageMetadata] = None): PackageMetadata = {
      overrideMetadata match {
        case Some(d) => PackageMetadata(d.name, PackageVersion(pulumiPackage.version).reconcile(d.version))
        case None    => PackageMetadata(pulumiPackage.name, PackageVersion(pulumiPackage.version))
      }
    }
  }

  def isMethod(functionDefinition: FunctionDefinition): Boolean =
    functionDefinition.inputs.properties.isDefinedAt(Utils.selfParameterName)

  def sha256(value: String): String = java.security.MessageDigest
    .getInstance("SHA-256")
    .digest(value.getBytes("UTF-8"))
    .map("%02x".format(_))
    .mkString
}
