package besom.codegen

import besom.codegen.UpickleApi.*

private case class PackageMetadataProtocol(
  name: String,
  version: Option[String] = None,
  server: Option[String] = None
)
object PackageMetadataProtocol {
  def derived: ReadWriter[PackageMetadataProtocol] = macroRW
}

case class PackageMetadata(
  name: PackageMetadata.SchemaName,
  version: Option[PackageVersion] = None,
  server: Option[String] = None
) {
  def withUrl(url: String): PackageMetadata = {
    val server = url match {
      case s"https://github.com/pulumi/pulumi-${_}" => None // use default
      case s"https://github.com/$org/$name"         => Some(s"github://api.github.com/$org/$name")
      case _                                        => throw GeneralCodegenException(s"Unknown repo url format: ${url}")
    }
    PackageMetadata(name, version, server)
  }

  def toJson: String = PackageMetadata.toJson(this)
}

object PackageMetadata {
  given ReadWriter[PackageMetadata] = PackageMetadataProtocol.derived.bimap(
    m => PackageMetadataProtocol(m.name, m.version.map(_.asString), m.server),
    p => PackageMetadata(p.name, PackageVersion(p.version), p.server)
  )

  type SchemaName    = String
  type SchemaVersion = String
  type SchemaFile    = os.Path

  def apply(name: SchemaName, version: SchemaVersion): PackageMetadata = {
    new PackageMetadata(name, PackageVersion(version), None)
  }

  def apply(name: SchemaName, version: SchemaVersion, server: String): PackageMetadata = {
    new PackageMetadata(name, PackageVersion(version), Some(server))
  }

  def fromJson(json: String): PackageMetadata = {
    read[PackageMetadata](json)
  }

  def fromJsonFile(path: os.Path): PackageMetadata = {
    read[PackageMetadata](os.read(path))
  }

  def toJson(m: PackageMetadata): String = write[PackageMetadata](m)
}

opaque type PackageVersion = PackageMetadata.SchemaVersion
object PackageVersion {

  private val DefaultVersion  = "0.0.0"
  val default: PackageVersion = new PackageVersion(DefaultVersion)

  private val SemverRegexPartial1 =
    """^v?(0|[1-9]\d*)""".r
  private val SemverRegexPartial2 =
    """^v?(0|[1-9]\d*)\.(0|[1-9]\d*)""".r
  private val SemverRegex =
    """^v?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$""".r

  def apply(version: Option[String]): Option[PackageVersion] = version.flatMap(apply)

  def apply(version: String): Option[PackageVersion] = version.trim.stripPrefix("v") match {
    case ""                            => None
    case DefaultVersion                => None
    case v @ SemverRegex(_*)           => Some(new PackageVersion(v))
    case v @ SemverRegexPartial1(_)    => Some(new PackageVersion(v + ".0.0")) // add minor and patch versions if missing
    case v @ SemverRegexPartial2(_, _) => Some(new PackageVersion(v + ".0")) // add patch version if missing
    case _                             => None
  }

  def unapply(version: PackageVersion): Option[String] = Some(version)

  implicit class PackageVersionOps(version: PackageVersion) {
    def asString: String = version
    override def toString: String = version.asString
  }

  implicit class PackageVersionOptionOps(version: Option[PackageVersion]) {
    def orDefault: PackageVersion = version.getOrElse(DefaultVersion)
    def isDefault: Boolean        = version.isEmpty || version.contains(DefaultVersion)

    def reconcile(otherVersion: Option[PackageVersion]): Option[PackageVersion] = {
      // make sure we parse first to normalize the versions
      (apply(version), apply(otherVersion)) match {
        case (Some(v), None)    => Some(v)
        case (Some(v), Some(_)) => Some(v) // we ignore the other version
        case (None, Some(v))    => Some(v)
        case (None, None)       => None
      }
    }
  }
}
