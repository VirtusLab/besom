package besom.internal

import scala.quoted.*

object OutputUnzip:
  inline def unzip[A <: NonEmptyTuple](output: Output[A]): Tuple.Map[A, Output] = ${ unzipImpl[A]('output) }

  // essentially we're performing Output[(A, B, C)] => (Output[A], Output[B], Output[C]) transformation
  def unzipImpl[A <: NonEmptyTuple: Type](outputA: Expr[Output[A]])(using Quotes): Expr[Tuple.Map[A, Output]] =
    import quotes.reflect.*

    // tuple xxl is represented as a linked list of types via AppliedType, we extract types recursively
    def extractTypesFromTupleXXL(tup: TypeRepr): List[TypeRepr] =
      tup match
        // tuple cons element
        case AppliedType(tpe, types) if tpe =:= TypeRepr.of[scala.*:] =>
          // for tuple cons, we expect exactly 2 types, type and tail consisting of another scala.*:
          types match
            case tpe :: tail :: Nil => tpe :: extractTypesFromTupleXXL(tail)
            case Nil                => Nil
            case _ =>
              report.errorAndAbort(s"Expected an AppliedType for scala.:* type (exactly 2 elems), got: ${types.map(_.show)}")
        // final element in the tuple
        case tpe if tpe =:= TypeRepr.of[EmptyTuple] => Nil
        case _                                      => report.errorAndAbort(s"Expected an AppliedType for scala.:* type, got: ${tup.show}")

    val tupleType = TypeRepr.of[A]
    val tupleTypes = tupleType match
      case AppliedType(tpe, types) if tpe =:= TypeRepr.of[scala.*:] => extractTypesFromTupleXXL(tupleType)
      case AppliedType(tpe, types)                                  => types
      case _ => report.errorAndAbort(s"Expected a tuple type, got: ${tupleType.show}")

    val mapExprs = tupleTypes.zipWithIndex.map { (tpe, idx) =>
      val idxExpr = Expr(idx)
      tpe.asType match
        case '[t] =>
          // we use Tuple#toArray to avoid _23 problem (compiler generates accessors up to 22 elems)
          '{ $outputA.map[t](x => x.toArray($idxExpr).asInstanceOf[t]) }
    }

    val tupleOfOutputs = mapExprs.foldLeft[Expr[Tuple]](Expr.ofTuple(EmptyTuple)) { (acc, expr) =>
      '{ $acc :* $expr }
    }

    '{ $tupleOfOutputs.asInstanceOf[Tuple.Map[A, Output]] }

  end unzipImpl

end OutputUnzip
