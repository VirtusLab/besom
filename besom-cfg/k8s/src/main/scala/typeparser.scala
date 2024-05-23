package besom.cfg

sealed trait Tpe:
  def stripOutputs: Tpe = this match
    case Tpe.Output(inner)  => inner.stripOutputs
    case Tpe.Struct(fields) => Tpe.Struct(fields.map((k, v) => (k, v.stripOutputs)))
    case Tpe.Lst(inner)     => Tpe.Lst(inner.stripOutputs)
    case Tpe.Union(types) =>
      val unionTypes = types.map(_.stripOutputs).distinct
      if unionTypes.length == 1 then unionTypes.head
      else Tpe.Union(unionTypes)
    case Tpe.Simple(name) => Tpe.Simple(name)

object Tpe:
  case class Simple(name: String) extends Tpe
  case class Lst(inner: Tpe) extends Tpe
  case class Union(types: List[Tpe]) extends Tpe
  case class Output(inner: Tpe) extends Tpe
  case class Struct(fields: List[(String, Tpe)]) extends Tpe:
    def keys: List[String]            = fields.map(_._1)
    def get(key: String): Option[Tpe] = fields.find(_._1 == key).map(_._2)

    // any list that isn't list of structs is a Val.Str with internal type interpolated
    // any struct is a Val.Map
    // any list of structs is a Val.List of Val.Map

    // we assume this is after stripping outputs
    def toValMap: ValMap =
      fields.foldLeft(ValMap.empty) { case (acc, (k, v)) =>
        acc.updated(
          k,
          v match
            case Tpe.Simple(name)                              => Val.Str(name)
            case Tpe.Lst(inner: Tpe.Struct)                    => Val.List(Val.Map(inner.toValMap))
            case Tpe.Lst(inner: Tpe.Simple)                    => Val.Str(s"List[${inner.name}]")
            case Tpe.Union(inner :: Tpe.Simple("Null") :: Nil) => throw Exception("Options are not supported yet")
            case Tpe.Lst(_)                                    => ??? // should not happen
            case Tpe.Union(types)  => ??? // should not happen, there are no instances of ConfiguredType for unions
            case Tpe.Output(inner) => ??? // should not happen
            case s: Tpe.Struct     => Val.Map(s.toValMap)
        )
      }

  import fastparse._, MultiLineWhitespace._

  def ident[$: P]: P[String]        = P(CharIn("a-zA-Z_") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!
  def simpleType[$: P]: P[Tpe]      = P((ident ~ !("[" | ".")).map(Tpe.Simple.apply))
  def structType[$: P]: P[Tpe]      = P("{" ~ field.rep(0) ~ "}").map(_.toList).map(Tpe.Struct.apply)
  def field[$: P]: P[(String, Tpe)] = P(ident ~ ":" ~ anyType)
  def anyType[$: P]: P[Tpe]         = P(unionType | outputType | structType | lstType | simpleType)
  def nonUnionAnyType[$: P]: P[Tpe] = P(outputType | structType | lstType | simpleType)
  def unionType[$: P]: P[Tpe]       = P(nonUnionAnyType.rep(2, "|")).map(_.toList).map(Tpe.Union.apply)
  def lstType[$: P]: P[Tpe]         = P("List[" ~ anyType ~ "]").map(Tpe.Lst.apply)
  def outputType[$: P]: P[Tpe]      = P("Output[" ~ anyType ~ "]").map(Tpe.Output.apply)

  def parseType(input: String): Either[Exception, Tpe] =
    parse(input, anyType(_)) match
      case Parsed.Success(value, _) => Right(value)
      case f: Parsed.Failure        => Left(Exception(f.trace().longMsg))
end Tpe
