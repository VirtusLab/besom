package besom.cfg

import scala.language.dynamics
import scala.quoted.*
import scala.collection.immutable.ListMap
import besom.cfg.internal.MetaUtils
import besom.types.{Output, Context}
import scala.util.chaining.*

// possible types under `Any`:
// simple types: Int, Long, Float, Double, String, Boolean
// complex types: List[A], Struct
// any of the above can be wrapped in Output
class Struct private (val _values: ListMap[String, Any]) extends Selectable:
  def selectDynamic(name: String) = _values(name)

  private[cfg] def fold[B](
    onStruct: Map[String, Output[B]] => Output[B],
    onList: List[B] => Output[B],
    onValue: Any => Output[B]
  )(using Context): Output[B] =
    val onOutput: Output[_] => Output[B] = _.flatMap {
      case s: Struct => s.fold(onStruct, onList, onValue)
      case a         => onValue(a)
    }

    val transformList: List[_] => Output[B] = l => { // v may be simple | Struct | Output, we don't support nested lists
      val outputOfVec = l.foldLeft(Output(Vector.empty[B])) { case (acc, v) =>
        acc.flatMap { accVec =>
          val transformedV = v match
            case s: Struct    => s.fold(onStruct, onList, onValue)
            case o: Output[_] => onOutput(o)
            case a            => onValue(a)

          transformedV.map(accVec :+ _)
        }
      }

      outputOfVec.map(_.toList).flatMap(onList)
    }

    _values.view
      .mapValues {
        case s: Struct  => s.fold[B](onStruct, onList, onValue)
        case i: List[_] => transformList(i)
        case o: Output[_] => // handle String -> Output[simple | Struct | List]
          o.flatMap {
            case s: Struct  => s.fold[B](onStruct, onList, onValue)
            case l: List[_] => transformList(l)
            case a          => onValue(a)
          }
        case a => onValue(a)
      }
      .to(ListMap)
      .pipe(onStruct)
  end fold
end Struct

object Struct extends Dynamic:
  def make(values: ListMap[String, Any]) = new Struct(values)

  inline def applyDynamic(apply: "apply")(): Struct = make(ListMap.empty)

  transparent inline def applyDynamicNamed(apply: "apply")(inline args: (String, Any)*): Struct =
    ${ applyDynamicImpl('args) }

  def applyDynamicImpl(args: Expr[Seq[(String, Any)]])(using Quotes): Expr[Struct] =
    import quotes.reflect.*

    type StructSubtype[T <: Struct] = T

    args match
      case Varargs(argExprs) =>
        val refinementTypes = argExprs.toList.map { case '{ ($key: String, $value: v) } =>
          (key.valueOrAbort, TypeRepr.of[v])
        }
        val exprs = argExprs.map { case '{ ($key: String, $value: v) } =>
          '{ ($key, $value) }
        }
        val argsExpr = Expr.ofSeq(exprs)

        MetaUtils.refineType(TypeRepr.of[Struct], refinementTypes).asType match
          case '[StructSubtype[t]] =>
            '{ Struct.make(${ argsExpr }.to(ListMap)).asInstanceOf[t] }

      case _ =>
        report.errorAndAbort(
          "Expected explicit varargs sequence. " +
            "Notation `args*` is not supported.",
          args
        )

  extension (s: Struct)
    def foldToEnv(using Context): Output[List[(String, String)]] = s.fold[List[(String, String)]](
      onStruct = { mapB =>
        mapB.foldLeft(Output(List.empty[(String, String)])) { case (acc, (k, v)) =>
          acc.flatMap { accList =>
            v.map { vList =>
              accList ++ vList.map { case (k2, v2) =>
                // println(s"struct, serializing '$k' '$k2' to ${if k2.isBlank() then s"$k -> $v2" else s"$k.$k2 -> $v2"}")
                if k2.isBlank then k -> v2 else s"$k.$k2" -> v2
              }
            }
          }
        }
      },
      onList = { list =>
        Output(list.zipWithIndex.flatMap { (lst, idx) =>
          lst.map { case (k, v) =>
            // println(s"list: serializing $k, $v to $k$idx -> $v")
            if k.isBlank() then s"$k$idx" -> v else s"$idx.$k" -> v
          }
        })
      },
      onValue = a =>
        // println(s"serializing $a to List(\"\" -> $a)")
        Output(List("" -> a.toString))
    )
end Struct
