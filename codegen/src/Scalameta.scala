package besom.codegen

import scala.meta.*
import scala.meta.dialects.Scala33

//noinspection ScalaFileName, TypeAnnotation
object scalameta {
  def parseStatement(code: String): Stat = code.parse[Stat].toEither match {
    case Left(error) => throw GeneralCodegenError(s"Failed to parse statement code:\n$code", error.details)
    case Right(term) => term
  }
  def parseSource(code: String): Source = code.parse[Source].toEither match {
    case Left(error) => throw GeneralCodegenError(s"Failed to parse source code:\n$code", error.details)
    case Right(term) => term
  }

  def ref(parts: String*): Term.Ref = ref(parts.toList)
  def ref(parts: List[String]): Term.Ref =
    parts.tail.foldLeft[Term.Ref](Term.Name(parts.head))((acc, name) => Term.Select(acc, Term.Name(name)))

  def given_(decltpe: Type)(body: Term) = Defn.GivenAlias(
    mods = Nil,
    name = Name.Anonymous(),
    paramClauseGroup = scala.None,
    decltpe = decltpe,
    body = body
  )

//  def def_(name: String, params: List[Term.Param], body: Term): Defn.Def         = Defn.Def(Nil, Term.Name(name), Nil, params, None, body)
//  def given_(name: String, params: List[Term.Param], body: Template): Defn.Given = Defn.GivenAlias(Nil, Term.Name(name), Nil, params, body)

  def apply_(qualType: Type.Name): Term.Ref             = apply_(Term.Name(qualType.value))
  def apply_(qual: Term.Ref): Term.Ref                  = Term.Select(qual, Term.Name("apply"))
  def method(qual: Term.Ref, name: String): Term.Ref    = method(qual, Term.Name(name))
  def method(qual: Term.Ref, name: Term.Name): Term.Ref = Term.Select(qual, name)

  val Unit: Term.Ref                        = Term.Select(Term.Name("scala"), Term.Name("Unit"))
  val None: Term.Ref                        = Term.Select(Term.Name("scala"), Term.Name("None"))
  val Some: Term.Ref                        = Term.Select(Term.Name("scala"), Term.Name("Some"))
  val List: Term.Ref                        = Term.Select(ref("scala", "collection", "immutable"), Term.Name("List"))
  def Some(value: Lit): Term.Apply          = Term.Apply(Some, Term.ArgClause(value :: Nil))
  def List(elements: List[Lit]): Term.Apply = Term.Apply(List, Term.ArgClause(elements))
  def List(elements: Lit*): Term.Apply      = List(elements.toList)

  object besom {
    object types {
      val Output: Term.Ref            = Term.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Term.Name("Output"))
      val Input: Term.Ref             = Term.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Term.Name("Input"))
      def Output(a: Term): Term.Apply = Term.Apply(Output, Term.ArgClause(a :: Nil))
      def Input(a: Term): Term.Apply  = Term.Apply(Input, Term.ArgClause(a :: Nil))
    }

    object internal {
      object CodegenProtocol {
        def apply(): Term.Ref = Term.Select(Term.Select(Term.Name("besom"), Term.Name("internal")), Term.Name("CodegenProtocol"))

        def jsonFormatN(size: Int)(a: Term): Term.Apply = Term.Apply(
          Term.Select(CodegenProtocol.apply(), Term.Name("jsonFormat" + size)),
          Term.ArgClause(a :: Nil)
        )
      }
    }
  }

  object types {
    private val Predef: Term.Ref = Term.Select(Term.Name("scala"), Term.Name("Predef"))

    val Boolean: Type.Ref = Type.Name("Boolean")
    val String: Type.Ref  = Type.Name("String")
    val Int: Type.Ref     = Type.Name("Int")
    val Double: Type.Ref  = Type.Name("Double")
    val Unit: Type.Ref    = Type.Select(Term.Name("scala"), Type.Name("Unit"))
    val Option: Type.Ref  = Type.Select(Term.Name("scala"), Type.Name("Option"))
    val List: Type.Ref    = Type.Select(ref("scala", "collection", "immutable"), Type.Name("List"))
    val Map: Type.Ref     = Type.Select(Predef, Type.Name("Map"))

    def Option(a: Type): Type.Apply       = Type.Apply(Option, Type.ArgClause(a :: Nil))
    def List(a: Type): Type.Apply         = Type.Apply(List, Type.ArgClause(a :: Nil))
    def Map(k: Type, v: Type): Type.Apply = Type.Apply(Map, Type.ArgClause(k :: v :: Nil))
    def Map(v: Type): Type.Apply          = Type.Apply(Map, Type.ArgClause(String :: v :: Nil))

    object besom {
      object types {
        val Context: Type.Ref = Type.Select(Term.Select(Term.Name("besom"), Term.Name("types")), Type.Name("Context"))
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
    }

    object spray {
      object json {
        def JsonFormat(a: Type): Type =
          Type.Apply(Type.Select(Term.Select(Term.Name("spray"), Term.Name("json")), Type.Name("JsonFormat")), Type.ArgClause(a :: Nil))
      }
    }
  }
}
