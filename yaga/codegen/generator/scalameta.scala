// Copied (and adapted) from codegen/**/scalameta.scala

package yaga.codegen.core.generator

import scala.meta.*
import scala.meta.dialects.Scala33
import scala.meta.parsers.Parse

object scalameta:
  def parseStatement(code: String): Stat = code.parse[Stat].toEither match
    case Left(error) => throw parseException(code, error)
    case Right(term) => term

  def parseSource(code: String): Source = code.parse[Source].toEither match
    case Left(error) => throw parseException(code, error)
    case Right(term) => term

  private def parseException(code: String, error: Parsed.Error): GeneralCodegenException =
    GeneralCodegenException(
      s"""Failed to parse source code: $error
         |Code:
         |${numberedLines(code)}
         |""".stripMargin,
      error.details
    )
  private def numberedLines(code: String): String =
    code.linesIterator.zipWithIndex.map { case (line, i) => f"${i + 1}%04d" + s"| $line" }.mkString("\n")

  def given_(decltpe: Type)(body: Term): Defn.GivenAlias = Defn.GivenAlias(
    mods = Nil,
    name = Name.Anonymous(),
    paramClauseGroup = scala.None,
    decltpe = decltpe,
    body = body
  )

  def apply_(qualType: Type.Name): Term.Ref             = apply_(Term.Name(qualType.value))
  def apply_(qual: Term.Ref): Term.Ref                  = Term.Select(qual, Term.Name("apply"))
  def method(qual: Term.Ref, name: String): Term.Ref    = method(qual, Term.Name(name))
  def method(qual: Term.Ref, name: Term.Name): Term.Ref = Term.Select(qual, name)

  def summon_(ts: Type*): Term.ApplyType = Term.ApplyType(Term.Name("summon"), Type.ArgClause(ts.toList))

  def ref(parts: String*): Term.Ref = ref(parts.toList)
  def ref(parts: List[String]): Term.Ref =
    parts.tail.foldLeft[Term.Ref](Term.Name(parts.head))((acc, name) => Term.Select(acc, Term.Name(name)))

  val Unit: Term.Ref                              = Term.Select(Term.Name("scala"), Term.Name("Unit"))
  val None: Term.Ref                              = Term.Select(Term.Name("scala"), Term.Name("None"))
  val Some: Term.Ref                              = Term.Select(Term.Name("scala"), Term.Name("Some"))
  val List: Term.Ref                              = Term.Select(ref("scala", "collection", "immutable"), Term.Name("List"))
  val Map: Term.Ref                               = Term.Select(ref("scala", "Predef"), Term.Name("Map"))
  def Some(value: Lit): Term.Apply                = Term.Apply(Some, Term.ArgClause(value :: Nil))
  def List(elements: List[Lit]): Term.Apply       = Term.Apply(List, Term.ArgClause(elements))
  def List(elements: Lit*): Term.Apply            = List(elements.toList)
  def Map(elements: List[Term.Tuple]): Term.Apply = Term.Apply(Map, Term.ArgClause(elements))

  object besom {
    object types {
      val Output: Term.Ref = Term.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Term.Name("Output"))
      val Input: Term.Ref  = Term.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Term.Name("Input"))

      def Output(a: Term): Term.Apply = Term.Apply(Output, Term.ArgClause(a :: Nil))
      def Input(a: Term): Term.Apply  = Term.Apply(Input, Term.ArgClause(a :: Nil))
    }

    object internal {
      object CodegenProtocol {
        def apply(): Term.Ref = Term.Select(Term.Select(Term.Name("besom"), Term.Name("internal")), Term.Name("CodegenProtocol"))

        def jsonFormatN: Term.Ref = Term.Select(CodegenProtocol.apply(), Term.Name("jsonFormatN"))
      }
    }
  }

  object types {
    private val Predef: Term.Ref = Term.Select(Term.Name("scala"), Term.Name("Predef"))

    val Boolean: Type.Ref  = Type.Name("Boolean")
    val String: Type.Ref   = Type.Name("String")
    val Byte: Type.Ref     = Type.Name("Byte")
    val Int: Type.Ref      = Type.Name("Int")
    val Long: Type.Ref     = Type.Name("Long")
    val Float: Type.Ref    = Type.Name("Float")
    val Double: Type.Ref   = Type.Name("Double")
    val Unit: Type.Ref     = Type.Select(Term.Name("scala"), Type.Name("Unit"))
    val Option: Type.Ref   = Type.Select(Term.Name("scala"), Type.Name("Option"))
    val Iterable: Type.Ref = Type.Select(ref("scala", "collection", "immutable"), Type.Name("Iterable"))
    val Map: Type.Ref      = Type.Select(Predef, Type.Name("Map"))

    def Option(a: Type): Type.Apply       = Type.Apply(Option, Type.ArgClause(a :: Nil))
    def Iterable(a: Type): Type.Apply     = Type.Apply(Iterable, Type.ArgClause(a :: Nil))
    def Map(k: Type, v: Type): Type.Apply = Type.Apply(Map, Type.ArgClause(k :: v :: Nil))
    def Map(v: Type): Type.Apply          = Type.Apply(Map, Type.ArgClause(String :: v :: Nil))

    def Union(ts: List[Type]): Type = ts.reduce((a, b) => Type.ApplyInfix(a, Type.Name("|"), b))

    object besom {
      // noinspection TypeAnnotation
      object types {
        def Output(a: Type): Type =
          Type.Apply(Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("Output")), Type.ArgClause(a :: Nil))
        def Input(a: Type): Type =
          Type.Apply(Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("Input")), Type.ArgClause(a :: Nil))
        def InputOptional(a: Type): Type =
          Type.Apply(Type.Select(Term.Select(ref("besom", "types"), Term.Name("Input")), Type.Name("Optional")), Type.ArgClause(a :: Nil))

        val Archive        = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("Archive"))
        val AssetOrArchive = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("AssetOrArchive"))
        val PulumiAny      = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("PulumiAny"))
        val PulumiJson     = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("PulumiJson"))
        val URN            = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("URN"))
        val ResourceId     = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("ResourceId"))
        val BooleanEnum    = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("BooleanEnum"))
        val IntegerEnum    = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("IntegerEnum"))
        val NumberEnum     = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("NumberEnum"))
        val StringEnum     = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("StringEnum"))
      }

      object internal {
        def Decoder(a: Type): Type =
          Type.Apply(Type.Select(Term.Select(Term.Name("besom"), Term.Name("internal")), Type.Name("Decoder")), Type.ArgClause(a :: Nil))
      }

      object json {
        def JsonFormat(a: Type): Type =
          Type.Apply(Type.Select(Term.Select(Term.Name("besom"), Term.Name("json")), Type.Name("JsonFormat")), Type.ArgClause(a :: Nil))
      }
    }
  }

  object interpolator:
    private def interleave[T](xs: List[T], ys: List[T]): List[T] = (xs, ys) match
      case (Nil, _)           => ys
      case (_, Nil)           => xs
      case (x :: xs, y :: ys) => x :: y :: interleave(xs, ys)

    implicit final class PulumiInterpolationOps(sc: StringContext):
      def meta(args: Any*): String =
        interleave(sc.parts.toList, args.toList).foldLeft("") { case (acc, e) =>
          e match
            case t: scala.meta.Tree => acc + t.syntax
            case s: String          => acc + s
            case o =>
              throw GeneralCodegenException(s"Unexpected type '${o.getClass.getTypeName}' passed to Besom Scala Meta interpolator: '${o}'")
        }

      def m(args: Any*): String = meta(args*)

end scalameta
