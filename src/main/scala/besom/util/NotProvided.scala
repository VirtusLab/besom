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

extension [F[+_], A](v: A | Output[F, A] | NotProvided)
  def asOutput(using ctx: Context.Of[F]): Output[F, A] =
    v match
      case NotProvided       => Output.empty
      case out: Output[_, _] => out.asInstanceOf[Output[F, A]] // TODO TypeTest?
      case a: A @unchecked   => Output(a)

extension [F[+_], A](v: Map[String, A] | Map[String, Output[F, A]] | Output[F, Map[String, A]] | NotProvided)
  def asOutputMap(using ctx: Context.Of[F]): Output[F, Map[String, A]] =
    v match
      case NotProvided       => Output.empty
      case out: Output[_, _] => out.asInstanceOf[Output[F, Map[String, A]]] // TODO TypeTest?
      case m: Map[_, _] @unchecked =>
        if m.exists((_, v) => v.isInstanceOf[Output[_, _]]) then
          Output.traverseMap(m.asInstanceOf[Map[String, Output[F, A]]])
        else Output(m.asInstanceOf[Map[String, A]])
