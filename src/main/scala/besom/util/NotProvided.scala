package besom.util

sealed trait NotProvided
case object NotProvided extends NotProvided

extension [A](v: A | NotProvided)
  def asOption: Option[A] =
    v match
      case NotProvided     => None
      case a: A @unchecked => Some(a)

import besom.internal.*
// import scala.reflect.TypeTest

// extension [F[+_], A](v: A | Output[F, A] | NotProvided)
//   def asOutput(using Context, Monad[F]): Output[F, A] =
//     v match
//       case NotProvided       => Output.empty
//       case out: Output[_, _] => out.asInstanceOf[Output[F, A]] // TODO TypeTest?
//       case a: A @unchecked   => Output(a)

extension [F[+_], A](value: A)
  def asOutput(using ol: OutputLifter[F, A], F: Monad[F], ctx: Context): ol.Out = ol.lift(value)

trait OutputLifter[F[+_], A]:
  type Out
  def lift(a: A)(using Monad[F], Context): Out

object OutputLifter extends OutputLifterGiven0:
  type Aux[F[+_], A, O] = OutputLifter[F, A] { type Out = O }

trait OutputLifterGiven0 extends OutputLifterGiven1:
  self: OutputLifter.type =>

  given [F[+_], A]: Aux[F, Output[F, A], Output[F, A]] = new OutputLifter[F, Output[F, A]]:
    type Out = Output[F, A]
    def lift(a: Output[F, A])(using Monad[F], Context): Output[F, A] = a

  given [F[+_], A]: Aux[F, NotProvided, Output[F, A]] = new OutputLifter[F, NotProvided]:
    type Out = Output[F, A]
    def lift(a: NotProvided)(using ctx: Context): Output[ctx.F, A] = Output.empty

trait OutputLifterGiven1:
  self: OutputLifter.type =>
  given [F[+_], A]: OutputLifter.Aux[F, A, Output[F, A]] = new OutputLifter[F, A]:
    type Out = Output[F, A]
    def lift(a: A)(using ctx: Context): Output[ctx.F, A] = Output(a)
