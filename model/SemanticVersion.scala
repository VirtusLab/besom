package besom.model

// TODO: move to separate module
// NOTICE: keep in sync with auto/src/main/scala/besom/model/SemanticVersion.scala

/** A semantic version as defined by https://semver.org/
  *
  * @param major
  *   Major version number
  * @param minor
  *   Minor version number
  * @param patch
  *   Patch version number
  * @param preRelease
  *   Pre-release version identifier
  * @param buildMetadata
  *   Build metadata version identifier
  */
case class SemanticVersion(
  major: Int,
  minor: Int,
  patch: Int,
  preRelease: Option[String] = None,
  buildMetadata: Option[String] = None
) extends Ordered[SemanticVersion]:
  require(major >= 0, "major version must be non-negative")
  require(minor >= 0, "minor version must be non-negative")
  require(patch >= 0, "patch version must be non-negative")

  override def compare(that: SemanticVersion): Int =
    import math.Ordered.orderingToOrdered

    val mainCompared = (major, minor, patch).compare((that.major, that.minor, that.patch))

    // for pre release compare each dot separated identifier from left to right
    lazy val thisPreRelease: Vector[Option[String]] = preRelease.map(_.split('.')).getOrElse(Array.empty[String]).toVector.map(Some(_))
    lazy val thatPreRelease: Vector[Option[String]] = that.preRelease.map(_.split('.')).getOrElse(Array.empty[String]).toVector.map(Some(_))

    def comparePreReleaseIdentifier(thisIdentifeir: Option[String], thatIdentifier: Option[String]): Int =
      (thisIdentifeir, thatIdentifier) match
        case (Some(thisId), Some(thatId)) =>
          val thisIsNumeric = thisId.forall(_.isDigit)
          val thatIsNumeric = thatId.forall(_.isDigit)
          (thisIsNumeric, thatIsNumeric) match
            case (true, true)   => thisId.toInt.compare(thatId.toInt)
            case (false, false) => thisId.compare(thatId)
            case (true, false)  => -1 // numeric identifiers have always lower precedence than non-numeric identifiers
            case (false, true)  => 1 // numeric identifiers have always lower precedence than non-numeric identifiers
        /* A larger set of pre-release fields has a higher precedence than a smaller set, if all of the preceding identifiers are equal. */
        case (Some(_), None) => 1 // larger set of pre-release fields has higher precedence
        case (None, Some(_)) => -1 // larger set of pre-release fields has higher precedence
        case (None, None)    => 0

    lazy val preCompared: Int =
      thisPreRelease
        .zipAll(thatPreRelease, None, None)
        .map(comparePreReleaseIdentifier.tupled)
        .find(_ != 0)
        .getOrElse(
          thisPreRelease.length.compare(thatPreRelease.length)
        ) // if all identifiers are equal, the version with fewer fields has lower precedence

    // ignore build metadata when comparing versions per semver spec https://semver.org/#spec-item-10

    if mainCompared != 0
    then mainCompared
    else if thisPreRelease.isEmpty && thatPreRelease.nonEmpty
    then 1 // normal version has higher precedence than a pre-release version
    else if thisPreRelease.nonEmpty && thatPreRelease.isEmpty
    then -1 // normal version has higher precedence than a pre-release version
    else preCompared // pre-release version has lower precedence than a normal version
  end compare

  def isSnapshot: Boolean = preRelease.contains("SNAPSHOT")

  lazy val preReleaseString: String    = preRelease.map("-" + _).getOrElse("")
  lazy val buildMetadataString: String = buildMetadata.map("+" + _).getOrElse("")

  override def toString: String = s"$major.$minor.$patch$preReleaseString$buildMetadataString"
  def toShortString: String =
    val xyz = List(major) ++ Option.when(minor > 0)(minor) ++ Option.when(patch > 0)(patch)
    xyz.mkString(".") + preReleaseString + buildMetadataString
end SemanticVersion

//noinspection ScalaFileName
object SemanticVersion {
  // https://semver.org/spec/v2.0.0.html#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
  private val versionRegex =
    """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$""".r

  def apply(major: Int, minor: Int, patch: Int): SemanticVersion =
    new SemanticVersion(major, minor, patch, None, None)

  def apply(major: Int, minor: Int, patch: Int, preRelease: String): SemanticVersion =
    new SemanticVersion(major, minor, patch, Some(preRelease), None)

  def apply(major: Int, minor: Int, patch: Int, preRelease: String, buildMetadata: String): SemanticVersion =
    new SemanticVersion(major, minor, patch, Some(preRelease), Some(buildMetadata))

  def parse(version: String): Either[Exception, SemanticVersion] = {
    version match {
      case versionRegex(major, minor, patch, preRelease, buildMetadata) =>
        Right(SemanticVersion(major.toInt, minor.toInt, patch.toInt, Option(preRelease), Option(buildMetadata)))
      case _ => Left(Exception(s"Cannot parse as semantic version: '$version'"))
    }
  }

  /** ParseTolerant allows for certain version specifications that do not strictly adhere to semver specs to be parsed by this library. It
    * does so by normalizing versions before passing them to [[parse]]. It currently trims spaces, removes a "v" prefix, and adds a 0 patch
    * number to versions with only major and minor components specified.
    */
  def parseTolerant(version: String): Either[Exception, SemanticVersion] = {
    val str = version.trim.stripPrefix("v")

    // Split into major.minor.(patch+pr+meta)
    val parts = str.split("\\.", 3)
    if parts.length < 3 then
      if parts.last.contains("+") || parts.last.contains("-") then
        Left(Exception("Short version cannot contain PreRelease/Build meta data"))
      else parse((parts.toList ::: List.fill(3 - parts.length)("0")).mkString("."))
    else parse(str)
  }
}
