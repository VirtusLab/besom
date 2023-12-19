package besom.model

//noinspection ScalaFileName
case class SemanticVersion(
  major: Int,
  minor: Int,
  patch: Int,
  preRelease: Option[String],
  buildMetadata: Option[String]
) extends Ordered[SemanticVersion]:

  override def compare(that: SemanticVersion): Int =
    import math.Ordered.orderingToOrdered

    val mainCompared = (major, minor, patch).compare((that.major, that.minor, that.patch))

    // for pre release compare each dot separated identifier from left to right
    val thisPreRelease = preRelease.map(_.split('.')).getOrElse(Array.empty[String])
    val thatPreRelease = that.preRelease.map(_.split('.')).getOrElse(Array.empty[String])
    val preCompared = thisPreRelease
      .zip(thatPreRelease)
      .map { case (thisId, thatId) =>
        if thisId.forall(_.isDigit) && thatId.forall(_.isDigit)
        then thisId.toInt.compare(thatId.toInt)
        else thisId.compare(thatId)
      }
      .find(_ != 0)
      .getOrElse(thisPreRelease.length.compare(thatPreRelease.length))
    // ignore build metadata when comparing versions per semver spec https://semver.org/#spec-item-10

    if mainCompared != 0
    then mainCompared
    else preCompared

  override def toString: String =
    val preReleaseString    = preRelease.map("-" + _).getOrElse("")
    val buildMetadataString = buildMetadata.map("+" + _).getOrElse("")
    s"$major.$minor.$patch$preReleaseString$buildMetadataString"

//noinspection ScalaFileName
object SemanticVersion {
  private val versionRegex = """(\d+)\.(\d+)\.(\d+)(?:-(.+))?(?:\+(.+))?""".r

  def apply(major: Int, minor: Int, patch: Int): SemanticVersion =
    SemanticVersion(major, minor, patch, None, None)

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
