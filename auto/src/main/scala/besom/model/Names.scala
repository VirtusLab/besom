package besom.model

import scala.compiletime.*
import scala.compiletime.ops.string.*
import scala.language.implicitConversions

/** Name is an identifier. */
opaque type Name <: String = String
object Name:
  private val NameFirstCharRegexpPattern = "[A-Za-z0-9_.-]"
  private val NameRestCharRegexpPattern  = "[A-Za-z0-9_.-]*"
  private[model] val NameRegexpPattern   = NameFirstCharRegexpPattern + NameRestCharRegexpPattern

  private val NameRegexp                 = NameRegexpPattern.r
  private[model] val NameFirstCharRegexp = ("^" + NameFirstCharRegexpPattern + "$").r
  private[model] val NameRestCharRegexp  = ("^" + NameRestCharRegexpPattern + "$").r

  /** IsName checks whether a string is a legal Name. */
  def isName(s: String): Boolean = s.nonEmpty && NameRegexp.findFirstIn(s).isDefined

  /** Parse a string into a [[Name]].
    * @param s
    *   is a string to parse
    * @return
    *   a [[Name]] if the string is valid, otherwise a compile time error occurs
    */
  inline def apply(s: String): Name =
    requireConst(s)
    inline if !constValue[Matches[s.type, "[A-Za-z0-9_.-][A-Za-z0-9_.-]*"]] then
      error("Invalid Name string. Must match '[A-Za-z0-9_.-][A-Za-z0-9_.-]*'.")
    else s

  implicit inline def str2Name(inline s: String): Name = Name(s)

  private[besom] def unsafeOf(s: String): Name = s

  extension (name: Name)
    /** Turns a [[Name]] into a qualified name, this is legal, since Name's is a proper subset of QName's grammar.
      * @return
      *   the [[Name]] as a [[QName]]
      */
    def asQName: QName = QName.unsafeOf(name)

end Name

/** QName is a qualified identifier. The "/" character optionally delimits different pieces of the name. Each element conforms to Name
  * regexp pattern. For example, "pulumi/pulumi/stack".
  */

opaque type QName <: String = String
object QName:
  /** Parse a string into a [[QName]].
    * @param s
    *   is a string to parse
    * @return
    *   a [[QName]] if the string is valid, otherwise a compile time error occurs
    */
  inline def apply(s: String): QName =
    requireConst(s)
    inline if !constValue[Matches[s.type, "([A-Za-z0-9_.-][A-Za-z0-9_.-]*/)*[A-Za-z0-9_.-][A-Za-z0-9_.-]*"]] then
      error("Invalid QName string. Must match '([A-Za-z0-9_.-][A-Za-z0-9_.-]*/)*[A-Za-z0-9_.-][A-Za-z0-9_.-]*'.")
    else s

  implicit inline def str2QName(inline s: String): QName = QName(s)

  private[besom] def unsafeOf(s: String): QName = s

  /** QNameDelimiter is what delimits Namespace and Name parts. */
  private val QNameDelimiter     = "/"
  private val QNameRegexpPattern = "(" + Name.NameRegexpPattern + "\\" + QNameDelimiter + ")*" + Name.NameRegexpPattern
  private val QNameRegexp        = QNameRegexpPattern.r

  /** IsQName checks whether a string is a legal QName. */
  def isQName(s: String): Boolean = s.nonEmpty && QNameRegexp.findFirstIn(s).isDefined

  /** Converts an arbitrary string into a [[QName]], converting the string to a valid [[QName]] if necessary. The conversion is
    * deterministic, but also lossy.
    */
  def parse(s: String): QName =
    val output = s.split(QNameDelimiter).filter(_.nonEmpty).map { segment =>
      val chars = segment.toCharArray
      if (!Name.NameFirstCharRegexp.matches(chars.head.toString)) chars.update(0, '_')
      for (i <- 1 until chars.length) {
        if (!Name.NameRestCharRegexp.matches(chars(i).toString)) chars.update(i, '_')
      }
      new String(chars)
    }
    val result = output.mkString(QNameDelimiter)
    if (result.isEmpty) QName.unsafeOf("_") else QName.unsafeOf(result)
  end parse

  extension (qname: QName)
    /** Extracts the [[Name]] portion of a [[QName]] (dropping any namespace). */
    def name: Name =
      val ix  = qname.lastIndexOf(QNameDelimiter)
      val nmn = if ix == -1 then qname else qname.substring(ix + 1)
      assert(Name.isName(nmn), s"QName $qname has invalid name $nmn")
      nmn

    /** Extracts the namespace portion of a [[QName]] (dropping the name), this may be empty. */
    def namespace: QName =
      val ix = qname.lastIndexOf(QNameDelimiter)
      val qn = if ix == -1 then "" else qname.substring(0, ix)
      assert(isQName(qn), s"QName $qname has invalid namespace $qn")
      QName.unsafeOf(qn)

end QName

/** PackageName is a qualified name referring to an imported package. */
type PackageName = QName
object PackageName:
  def parse(s: String): PackageName = QName.parse(s)

/** ModuleName is a qualified name referring to an imported module from a package. */
type ModuleName = QName

/** ModuleMemberName is a simple name representing the module member's identifier. */
type ModuleMemberName = Name

/** ClassMemberName is a simple name representing the class member's identifier. */
type ClassMemberName = Name

/** TypeName is a simple name representing the type's name, without any package/module qualifiers. */
type TypeName = Name
