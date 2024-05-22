package besom.model

import scala.compiletime.*
import scala.compiletime.ops.string.*
import scala.language.implicitConversions

/** A stack name formatted with the least possible specificity: `stack` or `org/stack` or `org/project/stack` or `user/project/stack`
  *
  * The stack name is specified in one of the following formats:
  *
  *   - `stack`: Identifies the stack name in the current user account or default organization, and the `project` specified by the nearest
  *     `Pulumi.yaml` project file.
  *   - `org/stack`: Identifies the stack stackName in the organization `org`, and the project specified by the nearest `Pulumi.yaml`
  *     project file.
  *   - `org/project/stack`: Identifies the `stack` name in the organization `org` and the `project`, `project` must match the project
  *     specified by the nearest `Pulumi.yaml` project file.
  *
  * For self-managed backends, the `org` portion of the stack name must always be the constant value `organization`.
  */
opaque type StackName <: String = String
object StackName extends StackNameFactory

trait StackNameFactory:

  def apply(stackName: String): StackName = parse(stackName) match
    case Right(stackName) => stackName
    case Left(e)          => throw e

  def parse(stackName: String): Either[Exception, StackName] =
    if stackName.nonEmpty
    then Right(stackName.asInstanceOf[StackName])
    else Left(Exception("StackName cannot be empty"))

  extension (stackName: StackName)
    /** @return
      *   the [[StackName]] as a [[Tuple]] of [[String]]s, in the format `(org, project, stack)`
      */
    def parts: (Option[String], Option[String], String) = stackName.split("/") match
      case Array(stack) if stack.nonEmpty               => (None, None, stack)
      case Array(project, stack) if stack.nonEmpty      => (None, Some(project), stack)
      case Array(org, project, stack) if stack.nonEmpty => (Some(org), Some(project), stack)
      case _                                            => throw IllegalArgumentException(s"Invalid StackName string: ${stackName}")

    /** @return
      *   the [[FullyQualifiedStackName]] for this stack name
      */
    def asFullyQualified(
      defaultOrganization: => String = "organization",
      defaultProject: => String
    ): FullyQualifiedStackName =
      parts match
        case (None, None, stack)               => FullyQualifiedStackName(defaultOrganization, defaultProject, stack)
        case (None, Some(project), stack)      => FullyQualifiedStackName(defaultOrganization, project, stack)
        case (Some(org), None, stack)          => FullyQualifiedStackName(org, defaultProject, stack)
        case (Some(org), Some(project), stack) => FullyQualifiedStackName(org, project, stack)

end StackNameFactory

/** A stack name formatted with the greatest possible specificity: `org/project/stack` or `user/project/stack`
  *
  * Using this format avoids ambiguity in stack identity guards creating or selecting the wrong stack. Note that filestate backends (local
  * file, S3, Azure Blob, etc.) do not support stack names, and instead prefixes the stack name with the word `organization` and the project
  * name, e.g. `organization/my-project-name/my-stack-name`.
  *
  * See: https://github.com/pulumi/pulumi/issues/2522
  */
opaque type FullyQualifiedStackName <: StackName = StackName
object FullyQualifiedStackName extends FullyQualifiedStackNameFactory

trait FullyQualifiedStackNameFactory:

  private val FullyQualifiedStackNamePattern = ".+/.+/.+".r

  /** Build a fully qualified stack name from its parts with default organization.
    *
    * @param project
    *   the project name
    * @param stack
    *   the stack name
    * @return
    */
  def apply(project: String, stack: String): FullyQualifiedStackName = FullyQualifiedStackName("organization", project, stack)

  /** Build a fully qualified stack name from its parts.
    *
    * @param org
    *   the organization name
    * @param project
    *   the project name
    * @param stack
    *   the stack name
    * @return
    *   a [[FullyQualifiedStackName]] if the string is valid, otherwise an exception is thrown
    */
  def apply(org: String, project: String, stack: String): FullyQualifiedStackName =
    val fqsn = s"${org}/${project}/${stack}"
    parse(fqsn) match
      case Right(fqsn) => fqsn
      case Left(e)     => throw e

  /** Parse a string into a [[FullyQualifiedStackName]].
    * @param s
    *   is a string to parse
    * @return
    *   a [[FullyQualifiedStackName]] if the string is valid, otherwise `None`
    */
  def parse(s: String): Either[Exception, FullyQualifiedStackName] =
    if FullyQualifiedStackNamePattern.matches(s) then Right(unsafeOf(s))
    else Left(Exception(s"Expected FullyQualifiedStackName to match '$FullyQualifiedStackNamePattern', got: '$s'"))

  /** Parse a string into a [[FullyQualifiedStackName]].
    *
    * @param s
    *   is a string to parse
    * @return
    *   a [[FullyQualifiedStackName]] if the string is valid, otherwise a compile time error occurs
    */
  inline def apply(s: String): FullyQualifiedStackName =
    requireConst(s)
    inline if !constValue[Matches[s.type, ".+/.+/.+"]] then error("Invalid FullyQualifiedStackName string. Must match '.+/.+/.+'")
    else s

  implicit inline def str2FullyQualifiedStackName(inline s: String): FullyQualifiedStackName = FullyQualifiedStackName(s)
  implicit inline def fqsn2Str(inline fqsn: String | FullyQualifiedStackName): String        = fqsn

  private[model] def unsafeOf(s: String): FullyQualifiedStackName =
    s.asInstanceOf[FullyQualifiedStackName] // FIXME: why this fails without cast?

  extension (fqsn: FullyQualifiedStackName)
    /** @return
      *   the [[FullyQualifiedStackName]] as a [[Tuple]] of [[String]]s, in the format `(org, project, stack)`
      */
    def parts: (String, String, String) = fqsn match
      case s"${org}/${project}/${stack}" => (org, project, stack)
      case _                             => throw IllegalArgumentException(s"Invalid FullyQualifiedStackName string: ${fqsn}")

    /** @return
      *   the organization name
      */
    def organization: String = parts._1

    /** @return
      *   the project name
      */
    def project: String = parts._2

    /** @return
      *   the stack name
      */
    def stack: String = parts._3

end FullyQualifiedStackNameFactory
