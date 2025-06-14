package besom.internal

import scala.quoted.*
import scala.deriving.*
import scala.reflect.Typeable
import scala.util.NotGiven
import besom.types.{Archive, AssetOrArchive, PulumiEnum}
import besom.types.Archive.*
import besom.json.JsValue

object Update extends UpdateLowPrioInstances1:
  trait LeafUpdate[A] extends Update[A]:
    def update(a: A, pf: PartialFunction[Any, Any]): A =
      if pf.isDefinedAt(a) then pf(a).asInstanceOf[A] else a.asInstanceOf[A]

  given stringUpdate: LeafUpdate[String] with {}
  given intUpdate: LeafUpdate[Int] with {}
  given longUpdate: LeafUpdate[Long] with {}
  given floatUpdate: LeafUpdate[Float] with {}
  given doubleUpdate: LeafUpdate[Double] with {}
  given booleanUpdate: LeafUpdate[Boolean] with {}
  given jsonUpdate: LeafUpdate[JsValue] with {}
  given assetOrArchiveUpdate[A <: AssetOrArchive]: LeafUpdate[A] with {}
  given pulumiEnumUpdate[A <: PulumiEnum[?]]: LeafUpdate[A] with {}

  // TODO should we apply pf to entire collections after their elements are updated?

  given outputUpdate[A](using inner: Update[A]): Update[Output[A]] with
    def update(output: Output[A], pf: PartialFunction[Any, Any]): Output[A] =
      output.map(a => inner.update(a, pf))

  given optionUpdate[A](using inner: Update[A]): Update[Option[A]] with
    def update(opt: Option[A], pf: PartialFunction[Any, Any]): Option[A] =
      opt.map(a => inner.update(a, pf))

  given listUpdate[A](using inner: Update[A]): Update[List[A]] with
    def update(list: List[A], pf: PartialFunction[Any, Any]): List[A] =
      list.map(a => inner.update(a, pf))

  given seqUpdate[A](using inner: Update[A]): Update[Seq[A]] with
    def update(seq: Seq[A], pf: PartialFunction[Any, Any]): Seq[A] =
      seq.map(a => inner.update(a, pf))

  given indexedSeqUpdate[A](using inner: Update[A]): Update[IndexedSeq[A]] with
    def update(seq: IndexedSeq[A], pf: PartialFunction[Any, Any]): IndexedSeq[A] =
      seq.map(a => inner.update(a, pf))

  given setUpdate[A](using inner: Update[A]): Update[Set[A]] with
    def update(set: Set[A], pf: PartialFunction[Any, Any]): Set[A] =
      set.map(a => inner.update(a, pf))

  given vectorUpdate[A](using inner: Update[A]): Update[Vector[A]] with
    def update(vector: Vector[A], pf: PartialFunction[Any, Any]): Vector[A] =
      vector.map(a => inner.update(a, pf))

  given mapUpdate[K, V](using inner: Update[V]): Update[Map[K, V]] with
    def update(map: Map[K, V], pf: PartialFunction[Any, Any]): Map[K, V] =
      map.map { case (k, v) => k -> inner.update(v, pf) }

  inline given [P <: Product](using m: Mirror.ProductOf[P]): Update[P] =
    ${ UpdateHelpers.genUpdateProductImpl('m) }

  given eitherUpdate[L, R](using lUpdate: Update[L], rUpdate: Update[R]): Update[Either[L, R]] with
    def update(either: Either[L, R], pf: PartialFunction[Any, Any]): Either[L, R] =
      either match
        case Left(l)  => Left(lUpdate.update(l, pf))
        case Right(r) => Right(rUpdate.update(r, pf))
end Update

trait Update[A]:
  def update(a: A, pf: PartialFunction[Any, Any]): A

trait UpdateLowPrioInstances1 extends UpdateLowPrioInstances2:
  given unionBooleanUpdate[A: Typeable](using notBoolean: NotGiven[A <:< Boolean], updateGeneric: Update[A]): Update[Boolean | A] =
    UpdateHelpers.unionUpdate2[A, Boolean](using updateGeneric, summon[Typeable[A]], Update.booleanUpdate, summon[Typeable[Boolean]])
  given unionStringUpdate[A: Typeable](using notString: NotGiven[A <:< String], updateGeneric: Update[A]): Update[String | A] =
    UpdateHelpers.unionUpdate2[A, String](using updateGeneric, summon[Typeable[A]], Update.stringUpdate, summon[Typeable[String]])
  given unionJsonUpdate[A: Typeable](using notJson: NotGiven[A <:< JsValue], updateGeneric: Update[A]): Update[JsValue | A] =
    UpdateHelpers.unionUpdate2[A, JsValue](using updateGeneric, summon[Typeable[A]], Update.jsonUpdate, summon[Typeable[JsValue]])

trait UpdateLowPrioInstances2:
  given singleOrIterableUpdate[A: Update: Typeable, L <: Iterable[?]: Update: Typeable]: Update[A | L] = UpdateHelpers.unionUpdate2[A, L]

object UpdateHelpers:
  def unionUpdate2[A, B](using aUpdate: Update[A], aTypeable: Typeable[A], bUpdate: Update[B], bTypeable: Typeable[B]): Update[A | B] =
    new Update[A | B]:
      override def update(a: A | B, pf: PartialFunction[Any, Any]): A | B =
        a match
          case a: A => aUpdate.update(a, pf).asInstanceOf[A]
          case b: B => bUpdate.update(b, pf).asInstanceOf[B]

  private[internal] def getUpdateInstances(tupledTypes: Type[?])(using Quotes): List[Expr[Update[?]]] =
    import quotes.reflect.*

    tupledTypes match
      case '[EmptyTuple] => Nil
      case '[tpe *: tail] =>
        val updateInstance = Expr
          .summon[Update[tpe]]
          .getOrElse(
            report.errorAndAbort(s"Could not find Update instance for type: ${Type.show[tpe]}")
          )
        updateInstance :: getUpdateInstances(Type.of[tail])

  def genUpdateProductImpl[A <: Product: Type](m: Expr[Mirror.ProductOf[A]])(using Quotes): Expr[Update[A]] =
    m match
      case '{
            $m: Mirror.ProductOf[A] { type MirroredElemLabels = elementLabels; type MirroredElemTypes = elementTypes }
          } =>
        '{
          new Update[A]:
            val instances = ${ Expr.ofList(getUpdateInstances(Type.of[elementTypes])) }
            def update(a: A, pf: PartialFunction[Any, Any]): A =
              val updatedFields = ${ m }.fromProduct(a).productIterator.zip(instances).map { case (value, updateInstance) =>
                updateInstance.asInstanceOf[Update[Any]].update(value, pf)
              }
              val withUpdatedFields = ${ m }.fromProduct(Tuple.fromArray(updatedFields.toArray))
              if pf.isDefinedAt(withUpdatedFields) then pf(withUpdatedFields).asInstanceOf[A]
              else withUpdatedFields
        }
