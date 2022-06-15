package besom.experimental

import scala.language.implicitConversions

import scala.quoted.*
import scala.deriving.Mirror

trait RefinedOutput[A]:
  type Refined

object RefinedOutput:
  transparent inline given refinedOutput[A]: RefinedOutput[A] = ${ refinedOutputImpl[A] }

  def refinedOutputImpl[A : Type](using quotes: Quotes) =
    import quotes.reflect.*

    val refinedType = Expr.summon[Mirror.Of[A]].get match
      case '{ $m: Mirror.ProductOf[A] { type MirroredElemLabels = elementLabels; type MirroredElemTypes = elementTypes } } =>
        refineType(TypeRepr.of[Output[A]], Type.of[elementLabels], Type.of[elementTypes]).asType

    refinedType match
      case '[t] => '{ new RefinedOutput[A] { type Refined = t } }


case class Output[+A](inner: A) extends Selectable:
  final def flatMap[B](f: A => Output[B]): Output[B] = f(inner)
  transparent inline def selectDynamic(inline name: String & Singleton) = ${ Output.selectDynamicImpl('this, 'name) }

object Output:
  implicit inline def refineOutput[A](output: Output[A])(using or: RefinedOutput[A]): or.Refined =
    output.asInstanceOf[or.Refined]

  def selectDynamicImpl[A : Type](output: Expr[Output[A]], name: Expr[String])(using quotes: Quotes): Expr[Any] =
    import quotes.reflect.*
    def selectName(inner: Expr[A]) = Select.unique(inner.asTerm, name.valueOrAbort).asExprOf[Output[Any]]
    '{ ${output}.flatMap(inner => ${selectName('{inner})}) }

def refineType(using quotes: Quotes)(base: quotes.reflect.TypeRepr, mirroredElemLabels: Type[?], mirroredElemTypes: Type[?]): quotes.reflect.TypeRepr =
  import quotes.reflect.*

  mirroredElemLabels match
    case '[EmptyTuple] => base
    case '[label *: labels] => mirroredElemTypes match
    case '[tpe *: tpes] =>
      val info = TypeRepr.of[tpe]
      val label = Type.valueOfConstant[label].get.toString
      val newBase = Refinement(base, label, info)
      refineType(newBase, Type.of[labels], Type.of[tpes])