package besom.cfg

import scala.collection.immutable.{ListMap, Set}
import besom.cfg.internal.*
import scala.quoted.*
import besom.types.Output

enum Val:
  case Str(value: String)
  case Map(value: ListMap[String, Val])
  case List(value: Val.Map)

type ValMap = ListMap[String, Val]

object ValMap:
  inline def empty: ValMap = ListMap.empty

private val NL = System.lineSeparator()

// TODO this is to be deprecated and replaced completely with Tpe.parseType
// TODO however, diff was written based on ValMap and Tpe is too precise (allows unions, Outputs)
// TODO we want to diff without outputs so current setup where we strip outputs from Tpe and then
// TODO convert to ValMap kinda works, but it's not ideal
def parseType(tpe: String): ValMap =
  val lines = tpe.split(NL).toVector.drop(1).dropRight(1) // drop the first and last line: { }
  def loop(lines: Vector[String], parent: ValMap): (Vector[String], ValMap) =
    lines match
      case Vector() => (Vector.empty, parent)
      case Vector(line, rest @ _*) =>
        val (key, value) = line.split(":").map(_.trim).toList match
          case key :: value :: Nil => (key, value)
          case "}" :: Nil          => ("", "}")
          case "}]" :: Nil         => ("", "}]")
          case _                   => throw Exception(s"Invalid line: $line")

        value match
          case "List[{" =>
            val (rest2, nested) = loop(rest.toVector, ValMap.empty)
            loop(rest2, parent + (key -> Val.List(Val.Map(nested))))
          case "{" =>
            val (rest2, nested) = loop(rest.toVector, ValMap.empty)
            loop(rest2, parent + (key -> Val.Map(nested)))
          case "}" =>
            (rest.toVector, parent)
          case "}]" =>
            (rest.toVector, parent)
          case _ =>
            loop(rest.toVector, parent + (key -> Val.Str(value)))

  val (rest, valmap) = loop(lines, ValMap.empty)
  assert(rest.isEmpty)
  valmap

def prettifyTypeString(tpe: String): String =
  tpe
    .replace("Predef.", "")
    .replace("scala.collection.immutable.", "")
    .replace("besom.internal.", "")
    .replace("besom.cfg.Struct ", "")
    .replace("besom.cfg.Struct", "{}") // empty Structs handled here
    .replace("scala.", "")
    .replace("java.lang.", "")
    .replace("val ", "")

def applyColorToTextOnly(str: fansi.Str, f: fansi.Str => fansi.Str): fansi.Str =
  val indent = str.toString.takeWhile(_.isWhitespace)
  val text = str.toString.dropWhile(_.isWhitespace)
  val colored = f(fansi.Str(text))
  fansi.Str(s"$indent$colored")

// rebuild the type string but mark actual differences with red, green and yellow colors
def diff(obtained: ValMap, expected: ValMap): fansi.Str =
  import fansi.Str
  import fansi.Color.{Green, Yellow, Red}

  def diffInternal(obtained: ValMap, expected: ValMap, indent: String = "  "): Vector[fansi.Str] =
    val keys = (expected.keys ++ obtained.keys).toVector.distinct
    keys.map { key =>
      val obtainedValue = obtained.get(key)
      val expectedValue = expected.get(key)

      (obtainedValue, expectedValue) match
        // same key and type on both sides, plain string
        case (Some(Val.Str(obtained)), Some(Val.Str(expected))) if obtained == expected =>
          Str(s"$indent$key: $obtained")

        // same key, different type (simple type)
        case (Some(Val.Str(obtained)), Some(Val.Str(expected))) =>
          val indentedKeyWithoutColor = Str(s"$indent$key: ")
          val redObtained = Red(s"got $obtained")
          val yelloExpected = Yellow(s", expected $expected")

          indentedKeyWithoutColor ++ redObtained ++ yelloExpected

        case (Some(Val.List(Val.Map(obtained))), Some(Val.List(Val.Map(expected)))) =>
          val nestedListDiff = diffInternal(obtained, expected, indent + "  ")
          val nestedListWrappedInBraces = nestedListDiff.mkString(s"List[{$NL", NL, s"$NL$indent}]")

          Str(s"$indent$key: ") ++ nestedListWrappedInBraces

        // same key, difference in a nested struct
        case (Some(Val.Map(obtained)), Some(Val.Map(expected))) =>
          val nestedStructDiff = diffInternal(obtained, expected, indent + "  ")
          val nestedStructWrappedInBraces = nestedStructDiff.mkString(s"{$NL", NL, s"$NL$indent}")

          Str(s"$indent$key: $nestedStructWrappedInBraces")

        // present in infra type, not present in application type, unnecessary addition (struct)
        case (Some(Val.Map(obtained)), None) =>
          val nestedStructDiff = diffInternal(obtained, ValMap.empty, indent + "  ")
          // inject Green in newlines because compiler strips it :(
          val nestedStructWrappedInBraces =
            nestedStructDiff
              .map(applyColorToTextOnly(_, Green.apply))
              .mkString(s"${Green("{")}$NL", NL, s"$NL$indent${Green("}")}")

          Green(s"$indent$key: ") ++ nestedStructWrappedInBraces

        // present in infra type, missing in application type, unnecessary addition (simple type)
        case (Some(Val.Str(obtained)), None) =>
          Green(s"$indent$key: $obtained")

        // present in infra type, missing in application type, unnecessary addition (list of structs)
        case (Some(Val.List(Val.Map(obtained))), None) =>
          val nestedListDiff = diffInternal(obtained, ValMap.empty, indent + "  ")
          val nestedListWrappedInBraces = nestedListDiff.mkString(s"${Green("List[{")}$NL", NL, s"$NL$indent${Green("}]")}")

          Green(s"$indent$key: ") ++ nestedListWrappedInBraces

        // present in application type, missing in infra type (simple type)
        case (None, Some(Val.Str(expected))) =>
          Red(s"$indent$key: $expected")

        // present in application type, missing in infra type (list of structs)
        case (None, Some(Val.List(Val.Map(expected)))) =>
          val nestedStructDiff = diffInternal(ValMap.empty, expected, indent + "  ")
          // inject Red in newlines because compiler strips it :(
          val nestedStructWrappedInBraces =
            nestedStructDiff
              .map(applyColorToTextOnly(_, Red.apply))
              .mkString(s"${Red("List[{")}$NL", NL, s"$NL$indent${Red("}]")}")

          Red(s"$indent$key: ") ++ nestedStructWrappedInBraces

        // present in application type, missing in infra type (struct)
        case (None, Some(Val.Map(expected))) =>
          val nestedStructDiff = diffInternal(ValMap.empty, expected, indent + "  ")
          // inject Red in newlines because compiler strips it :(
          val nestedStructWrappedInBraces =
            nestedStructDiff
              .map(applyColorToTextOnly(_, Red.apply))
              .mkString(s"${Red("{")}$NL", NL, s"$NL$indent${Red("}")}")

          Red(s"$indent$key: ") ++ nestedStructWrappedInBraces

        // obtained simple type, expected struct
        case (Some(Val.Str(obtained)), Some(Val.Map(expected))) =>
          val indentedKeyWithoutColor = Str(s"$indent$key: ")
          val redObtained = Red(s"got $obtained")
          val nestedStructDiff = diffInternal(ValMap.empty, expected, indent + "  ")
          // inject Yellow in newlines because compiler strips it :(
          val nestedStructWrappedInBraces =
            nestedStructDiff
              .map(applyColorToTextOnly(_, Yellow.apply))
              .mkString(s"${Yellow("{")}$NL", NL, s"$NL$indent${Yellow("}")}")

          val yelloExpected = Yellow(s", expected ") ++ nestedStructWrappedInBraces

          indentedKeyWithoutColor ++ redObtained ++ yelloExpected

        // obtained struct, expected list of structs
        case (Some(Val.Map(obtained)), Some(Val.List(Val.Map(expected)))) =>
          val indentedKeyWithoutColor = Str(s"$indent$key: ")
          val nestedObtainedStructDiff = diffInternal(obtained, ValMap.empty, indent + "  ")
          val nestedObtainedStructWrappedInBraces =
            nestedObtainedStructDiff
              .map(applyColorToTextOnly(_, Red.apply))
              .mkString(s"${Red("got {")}$NL", NL, s"$NL$indent${Red("}")}")

          val nestedExpectedListOfStructsDiff = diffInternal(ValMap.empty, expected, indent + "  ")
          val nestedExpectedListOfStructsWrappedInBraces =
            nestedExpectedListOfStructsDiff
              .map(applyColorToTextOnly(_, Yellow.apply))
              .mkString(s"${Yellow(", expected List[{")}$NL", NL, s"$NL$indent${Yellow("}]")}")

          indentedKeyWithoutColor ++ nestedObtainedStructWrappedInBraces ++ nestedExpectedListOfStructsWrappedInBraces

        // obtained simple type, expected list of structs
        case (Some(Val.Str(obtained)), Some(Val.List(Val.Map(expected)))) =>
          val indentedKeyWithoutColor = Str(s"$indent$key: ")
          val redObtained = Red(s"got $obtained")
          val nestedStructDiff = diffInternal(ValMap.empty, expected, indent + "  ")
          val nestedStructWrappedInBraces =
            nestedStructDiff
              .map(applyColorToTextOnly(_, Yellow.apply))
              .mkString(s"${Yellow("{")}$NL", NL, s"$NL$indent${Yellow("}]")}")

          val yelloExpected = Yellow(s", expected List[") ++ nestedStructWrappedInBraces

          indentedKeyWithoutColor ++ redObtained ++ yelloExpected

        // obtained list of structs, expected simple type
        case (Some(Val.List(Val.Map(obtained))), Some(Val.Str(expected))) =>
          val nestedListDiff = diffInternal(ValMap.empty, obtained, indent + "  ")
          val nestedListWrappedInBraces = nestedListDiff.mkString(s"${Red("got List[{")}$NL", NL, s"$NL$indent${Red("}]")}")

          val yelloExpected = Yellow(s", expected $expected")

          Str(s"$indent$key: ") ++ nestedListWrappedInBraces ++ yelloExpected

        // obtained list of structs, expected struct
        case (Some(Val.List(Val.Map(obtained))), Some(Val.Map(expected))) =>
          val indentedKeyWithoutColor = Str(s"$indent$key: ")
          val nestedObtainedStructDiff = diffInternal(obtained, ValMap.empty, indent + "  ")
          val nestedObtainedStructWrappedInBraces =
            nestedObtainedStructDiff
              .map(applyColorToTextOnly(_, Red.apply))
              .mkString(s"${Red("got List[{")}$NL", NL, s"$NL$indent${Red("}]")}")

          val nestedExpectedListOfStructsDiff = diffInternal(ValMap.empty, expected, indent + "  ")
          val nestedExpectedListOfStructsWrappedInBraces =
            nestedExpectedListOfStructsDiff
              .map(applyColorToTextOnly(_, Yellow.apply))
              .mkString(s"${Yellow(", expected {")}$NL", NL, s"$NL$indent${Yellow("}")}")

          indentedKeyWithoutColor ++ nestedObtainedStructWrappedInBraces ++ nestedExpectedListOfStructsWrappedInBraces

        // obtained struct, expected simple type
        case (Some(Val.Map(obtained)), Some(Val.Str(expected))) =>
          val indentedKeyWithoutColor = Str(s"$indent$key: ")
          val nestedStructDiff = diffInternal(obtained, ValMap.empty, indent + "  ")
          val nestedStructWrappedInBraces =
            nestedStructDiff
              .map(applyColorToTextOnly(_, Red.apply))
              .mkString(s"${Red("got {")}$NL", NL, s"$NL$indent${Red("}")}")

          val yelloExpected = Yellow(s", expected $expected")

          indentedKeyWithoutColor ++ nestedStructWrappedInBraces ++ yelloExpected
        // impossible state
        case (None, None) => throw Exception(s"Invalid state: $key is missing on both sides")
    }

  s"{$NL" + diffInternal(obtained, expected).mkString(NL) + s"$NL}"

object Diff:
  def performDiff[C <: Struct: Type](schema: Schema, configuration: Expr[C])(using Quotes): Either[fansi.Str, Unit] =
    import quotes.reflect.*

    // TODO this generates really f****d up types like:
    // shouldBeListOfStructsButItsAStruct: List[{
    //   x: Int | Output[Int]
    //   y: Double | Output[Double]
    // } | Output[{
    //   x: Int | Output[Int]
    //   y: Double | Output[Double]
    // }]] | List[Output[{
    //   x: Int | Output[Int]
    //   y: Double | Output[Double]
    // } | Output[{
    //   x: Int | Output[Int]
    //   y: Double | Output[Double]
    // }]]]
    def fieldTypeToTypeRepr(fieldType: FieldType): TypeRepr =
      fieldType match
        case FieldType.Int     => TypeRepr.of[scala.Int | Output[scala.Int]]
        case FieldType.Long    => TypeRepr.of[scala.Long | Output[scala.Long]]
        case FieldType.String  => TypeRepr.of[String | Output[String]]
        case FieldType.Boolean => TypeRepr.of[scala.Boolean | Output[scala.Boolean]]
        case FieldType.Float   => TypeRepr.of[scala.Float | Output[scala.Float]]
        case FieldType.Double  => TypeRepr.of[scala.Double | Output[scala.Double]]
        case FieldType.Optional(inner) => // TODO this is borked
          fieldTypeToTypeRepr(inner).asType match
            case '[t] => TypeRepr.of[t | Null]
        case FieldType.Array(inner) =>
          fieldTypeToTypeRepr(inner).asType match
            case '[t] => TypeRepr.of[List[t] | List[Output[t]]]
        case FieldType.Struct(fields: _*) =>
          val refinements = fields.map { (name, fieldType) =>
            val typeRepr = fieldTypeToTypeRepr(fieldType)
            name -> typeRepr
          }.toList

          MetaUtils.refineType(TypeRepr.of[Struct], refinements).asType match
            case '[t] => TypeRepr.of[t | Output[t]]

    def dealiasAll(tpe: TypeRepr): TypeRepr =
      tpe match
        case AppliedType(tycon, args) => AppliedType(dealiasAll(tycon), args.map(dealiasAll(_)))
        case _                        => tpe.dealias

    def schemaToTypeRepr(schema: Schema): TypeRepr =
      val refinements = schema.fields.map { case Field(name, fieldType) =>
        val typeRepr = fieldTypeToTypeRepr(fieldType)
        name -> typeRepr
      }.toList

      MetaUtils.refineType(TypeRepr.of[Struct], refinements)

    val expectedConfigTypeRepr = schemaToTypeRepr(schema)
    val obtainedTypeRepr = TypeRepr.of[C]

    val dealiasedExpectedConfigTypeRepr = dealiasAll(expectedConfigTypeRepr)
    val dealiasedObtainedTypeRepr = dealiasAll(obtainedTypeRepr)

    val expectedTypeString = dealiasedExpectedConfigTypeRepr.show
    val obtainedTypeString = dealiasedObtainedTypeRepr.show

    val expectedConfigType = expectedConfigTypeRepr.asType

    expectedConfigType match
      case '[t] =>
        type Expected = t

        configuration match
          case '{ $c: Expected } =>
            Right(())
          case '{ $c: cType } =>
            val prettyExpected = prettifyTypeString(expectedTypeString)
            val expected = Tpe.parseType(prettyExpected).map(_.stripOutputs) match
              case Left(ex)                  => throw ex
              case Right(st @ Tpe.Struct(_)) => st.toValMap
              case Right(_)                  => ??? // TODO should not happen, top levels are always structs

            val prettyObtained = prettifyTypeString(obtainedTypeString)
            val obtained = Tpe.parseType(prettyObtained).map(_.stripOutputs) match
              case Left(ex)                  => throw ex
              case Right(st @ Tpe.Struct(_)) => st.toValMap
              case Right(_)                  => ??? // TODO should not happen, top levels are always structs

            val prettyDiff = diff(
              obtained = obtained,
              expected = expected
            )

            Left(prettyDiff)
