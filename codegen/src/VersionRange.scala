package besom.codegen

import besom.model.SemanticVersion

sealed trait VersionRange:
  def matches(version: SemanticVersion): Boolean

case class SingleVersion(version: SemanticVersion) extends VersionRange:
  def matches(version: SemanticVersion): Boolean = this.version == version

case class ExactRange(from: SemanticVersion, to: SemanticVersion) extends VersionRange:
  def matches(version: SemanticVersion): Boolean =
    version >= from && version <= to

case class WildcardVersion(major: Int, minorPattern: Option[Int], patchPattern: Option[Int]) extends VersionRange:
  def matches(version: SemanticVersion): Boolean =
    version.major == major &&
      minorPattern.forall(_ == version.minor) &&
      patchPattern.forall(_ == version.patch)

case class FromVersion(from: SemanticVersion) extends VersionRange:
  def matches(version: SemanticVersion): Boolean =
    version >= from

object VersionRange:
  def parse(range: String): Either[Exception, VersionRange] =
    if range.startsWith("^") then
      val version = SemanticVersion.parseTolerant(range.stripPrefix("^"))
      version.map(FromVersion(_))
    else if range.contains(":") then
      // Handle range with colon e.g. "1.0.0:1.0.2"
      val parts = range.split(":")
      if parts.length != 2 then Left(Exception(s"Invalid version range format: $range. Expected format: version:version"))
      else
        for
          from <- SemanticVersion.parseTolerant(parts(0))
          to   <- SemanticVersion.parseTolerant(parts(1))
        yield ExactRange(from, to)
    else if range.contains("x") || range.contains("X") then
      // Handle wildcard version e.g. "1.x.x" or "1.2.x"
      val parts = range.toLowerCase.split("\\.")
      if parts.length != 3 then
        Left(Exception(s"Invalid wildcard version format: $range. Expected format: major.minor.patch with 'x' as wildcard"))
      else
        try
          val major = parts(0).toInt
          val minor = if parts(1) == "x" then None else Some(parts(1).toInt)
          val patch = if parts(2) == "x" then None else Some(parts(2).toInt)
          Right(WildcardVersion(major, minor, patch))
        catch
          case e: NumberFormatException =>
            Left(Exception(s"Invalid version number in range: $range", e))
    else
      // Handle single version e.g. "1.0.0"
      SemanticVersion.parseTolerant(range).map(SingleVersion(_))
