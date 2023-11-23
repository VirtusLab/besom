package besom.codegen

import besom.codegen.UpickleApi._

case class PackageMetadata private[codegen] (
  name: PackageMetadata.SchemaName,
  version: PackageVersion.PackageVersion,
  server: Option[String] = None
) {
  def withUrl(url: String): PackageMetadata = {
    val server = url match {
      case s"https://github.com/pulumi/pulumi-${_}" => None // use default
      case s"https://github.com/$org/$name"         => Some(s"github://api.github.com/$org/$name")
      case _                                        => throw GeneralCodegenError(s"Unknown repo url format: ${url}")
    }
    PackageMetadata(name, version, server)
  }

  def toJson: String = PackageMetadata.toJson(this)
}

object PackageMetadata {
  implicit val rw: ReadWriter[PackageMetadata] = macroRW

  type SchemaName    = String
  type SchemaVersion = String
  type SchemaFile    = os.Path

  def apply(name: SchemaName, version: SchemaVersion, server: Option[String] = None): PackageMetadata = {
    new PackageMetadata(name, PackageVersion(version), server)
  }

  def fromJson(json: String): PackageMetadata = {
    read[PackageMetadata](json)
  }

  def fromJsonFile(path: os.Path): PackageMetadata = {
    read[PackageMetadata](os.read(path))
  }

  def toJson(m: PackageMetadata): String = write[PackageMetadata](m)
}

object PackageVersion {
  type PackageVersion = PackageMetadata.SchemaVersion

  private val DefaultVersion  = "0.0.0"
  val default: PackageVersion = new PackageVersion(DefaultVersion)

  private val SemverRegexPartial1 =
    """^v?(0|[1-9]\d*)""".r
  private val SemverRegexPartial2 =
    """^v?(0|[1-9]\d*)\.(0|[1-9]\d*)""".r
  private val SemverRegex =
    """^v?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$""".r

  def apply(version: String): PackageVersion = parse(version).getOrElse(
    throw GeneralCodegenError(s"Invalid version format: '$version'")
  )

  def parse(version: String): Option[PackageVersion] = version.trim.stripPrefix("v") match {
    case ""                         => None
    case v @ SemverRegex(_*)        => Some(new PackageVersion(v))
    case v @ SemverRegexPartial1(_) => Some(new PackageVersion(v + ".0.0")) // add minor and patch versions if missing
    case v @ SemverRegexPartial2(_, _) => Some(new PackageVersion(v + ".0")) // add patch version if missing
    case _                             => None
  }

  implicit class PackageVersionOps(version: PackageVersion.PackageVersion) {
    def isDefault: Boolean = version == DefaultVersion

    def reconcile(otherVersion: PackageVersion.PackageVersion)(implicit logger: Logger): PackageVersion = {
      (version.isDefault, otherVersion.isDefault) match {
        case (false, true)  => version
        case (false, false) => version // we ignore the other version
        case (true, false)  => PackageVersion(otherVersion) // make sure we pass strings through the apply method
        case (true, true)   =>
          // this code should be only reached in tests
          logger.warn(s"Using default version '${DefaultVersion}' for the package - suitable only for tests")
          PackageVersion.default
      }
    }
  }
}
