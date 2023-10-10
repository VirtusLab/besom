package besom.internal

import scala.util.{NotGiven => Not}
import besom.util.NotProvided
import scala.collection.BuildFrom
import scala.quoted.*

/** Output is a wrapper for a monadic effect used to model async execution that allows Pulumi to track information about
  * dependencies between resources and properties of data (whether it's known or a secret for instance).
  *
  * Invariant: dataResult has to be registered in TaskTracker by the time it reaches the constructor here!
  * @param dataResult
  *   Effect of type Result[A]
  * @param ctx
  *   Context
  */
class Output[+A] private[internal] (using private[besom] val ctx: Context)(
  private val dataResult: Result[OutputData[A]]
):
  def map[B](f: A => B): Output[B] = Output.ofData(dataResult.map(_.map(f)))

  def flatMap[B](f: A => Output[B]): Output[B] =
    Output.ofData(
      for
        outputData: OutputData[A]         <- dataResult
        nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => f(a).getData)
      yield nested.flatten
    )

  inline def withFilter[B](inline p: A => Boolean): Output[A] =
    ${ Output.outputWithFilterImpl('this, 'p) }

  // EXTREMELY EXPERIMENTAL
  def flatMap[F[_]: Result.ToFuture, B](f: A => F[B]): Output[B] =
    Output.ofData(
      for
        outputData: OutputData[A]         <- dataResult
        nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => Result.eval(f(a)).map(OutputData(_)))
      yield nested.flatten
    )

  def zip[B](that: => Output[B])(using z: Zippable[A, B]): Output[z.Out] =
    Output.ofData(dataResult.zip(that.getData).map((a, b) => a.zip(b)))

  def flatten[B](using ev: A <:< Output[B]): Output[B] = flatMap(a => ev(a))

  def asPlaintext: Output[A] = withIsSecret(Result.pure(false))

  def asSecret: Output[A] = withIsSecret(Result.pure(true))

  private[internal] def getData: Result[OutputData[A]] = dataResult

  private[internal] def getValue: Result[Option[A]] = dataResult.map(_.getValue)

  private[internal] def getValueOrElse[B >: A](default: => B): Result[B] =
    dataResult.map(_.getValueOrElse(default))

  private[internal] def getValueOrFail(msg: String): Result[A] =
    dataResult.flatMap {
      case OutputData.Known(_, _, Some(value)) => Result.pure(value)
      case _                                   => Result.fail(Exception(msg))
    }

  private[internal] def withIsSecret(isSecretEff: Result[Boolean]): Output[A] =
    Output.ofData(
      for
        secret <- isSecretEff
        o      <- dataResult
      yield o.withIsSecret(secret)
    )

/** These factory methods should be the only way to create Output instances in user code!
  */
trait OutputFactory:
  def eval[F[_]: Result.ToFuture, A](value: F[A])(using Context): Output[A] = Output.eval(value)
  def apply[A](value: A)(using Context): Output[A]                          = Output(value)
  def secret[A](value: A)(using Context): Output[A]                         = Output.secret(value)
  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Output[A]]
  )(using BuildFrom[CC[Output[A]], A, To], Context): Output[To] = Output.sequence(coll)
  def traverse[A, CC[X] <: IterableOnce[X], B, To](
    coll: CC[A]
  )(
    f: A => Output[B]
  )(using BuildFrom[CC[Output[B]], B, To], Context): Output[To] = sequence(coll.map(f).asInstanceOf[CC[Output[B]]])
trait OutputExtensionsFactory:
  implicit final class OutputSequenceOps[A, CC[X] <: IterableOnce[X], To](coll: CC[Output[A]]):
    def sequence(using BuildFrom[CC[Output[A]], A, To], Context): Output[To] =
      Output.sequence(coll)
  implicit final class OutputTraverseOps[A, CC[X] <: IterableOnce[X]](coll: CC[A]):
    def traverse[B, To](f: A => Output[B])(using BuildFrom[CC[Output[B]], B, To], Context): Output[To] =
      coll.map(f).asInstanceOf[CC[Output[B]]].sequence

object Output:
  // should be NonEmptyString
  def traverseMap[A](using ctx: Context)(map: Map[String, Output[A]]): Output[Map[String, A]] =
    sequence(map.map((key, value) => value.map(result => (key, result))).toVector).map(_.toMap)

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Output[A]]
  )(using bf: BuildFrom[CC[Output[A]], A, To], ctx: Context): Output[To] =
    coll.iterator
      .foldLeft(Output(bf.newBuilder(coll))) { (acc, curr) =>
        acc.zip(curr).map { case (b, r) => b += r }
      }
      .map(_.result())

  def empty(isSecret: Boolean = false)(using ctx: Context): Output[Nothing] =
    new Output(ctx.registerTask(Result.pure(OutputData.empty[Nothing](isSecret = isSecret))))

  def eval[F[_]: Result.ToFuture, A](value: F[A])(using
    ctx: Context
  ): Output[A] =
    new Output[A](ctx.registerTask(Result.eval(value)).map(OutputData(_)))

  def apply[A](value: => Result[A])(using
    ctx: Context
  ): Output[A] =
    new Output[A](ctx.registerTask(OutputData.traverseResult(value)))

  // TODO could this be pure without implicit Context? it's not async in any way so? only test when all tests are written
  def apply[A](value: A)(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(OutputData(value))))

  def ofData[A](value: => Result[OutputData[A]])(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask((value)))

  // TODO could this be pure without implicit Context? it's not async in any way so? only test when all tests are written
  def ofData[A](data: OutputData[A])(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(data)))

  def secret[A](using ctx: Context)(value: A): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(OutputData(value))))

  def secret[A](using ctx: Context)(maybeValue: Option[A]): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(OutputData(Set.empty, maybeValue, isSecret = true))))

  /**
    * Make the compilation fail if the method is used in any way other than just
    * ```
    * IgnoredOutput(_) <- outputOfAnOutput
    * ```
    */
  def outputWithFilterImpl[A: Type](self: Expr[Output[A]], p: Expr[A => Boolean])(using Quotes): Expr[Output[A]] = {
    import quotes.reflect.*
    val outputType = TypeRepr.of[Output[A]]
    val outputSymbol = outputType.typeSymbol
    val ignoredOutputSymbol = Symbol.requiredModule("besom.util.IgnoredOutput")
    val ignoredOutputUnapplySymbol = ignoredOutputSymbol.companionModule.memberMethod("unapply").head

    def cantUseWithFilterError(): Nothing =
      report.errorAndAbort(s"Output.withFilter can only be used as a way to ignore Output values, got: ${pprint(p.asTerm)}", p)

    val defrhs = p.asTerm match {
      case Inlined(_, _, term@Block(List(defdef@DefDef(_, _, _, Some(defrhs))), _))
      if defdef.symbol.flags.is(Flags.Synthetic) =>
        defrhs
      case _ =>
        cantUseWithFilterError()
    }

    def isGeneratedIgnoredOutputMatch(tree: Tree): Boolean = tree match {
      case Match(_, cases) =>
        // cases.map(_.pattern).exists {
        //   case Typed(Unapply(TypeApply(Select(term, "unapply"), _), _, _), _)
        //   if term.toString.contains("IgnoredOutput")/*term.tpe <:< ignoredOutputSymbol.typeRef*/ =>
        //     true
        //   case Unapply(TypeApply(Select(term, "unapply"), _), _, _)
        //   if term.toString.contains("IgnoredOutput")/*term.tpe <:< ignoredOutputSymbol.typeRef*/ =>
        //     true
        //   case expr: Typed => // this doesn't match:
        //     // Typed(UnApply(TypeApply(Select(Ident(IgnoredOutput),unapply),List(TypeTree[TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class <root>)),object scala),Any)])),List(),List(Ident(_))),TypeTree[AppliedType(TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class besom)),object internal),Output),List(TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class <root>)),object scala),Any)))])
        //     // :/ WTF not?!
        //     println("1")
        //     println((expr.expr))
        //     true
        //   case tree =>
        //     println("2")
        //     println(tree.getClass())
        //     println((tree))
        //     false
        // }
        cases.map(_.pattern).exists(_.toString().contains("IgnoredOutput")) // I'm sorry
      case _ =>
        false
    }

    if isGeneratedIgnoredOutputMatch(defrhs) then
      self
    else
      cantUseWithFilterError()
  }
