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

extension [F[+_], A](v: A | Output[A] | NotProvided)
  def asOutput(using ctx: Context): Output[A] =
    v match
      case NotProvided     => Output.empty
      case out: Output[_]  => out.asInstanceOf[Output[A]] // TODO TypeTest?
      case a: A @unchecked => Output(a)

extension [F[+_], A](v: Map[String, A] | Map[String, Output[A]] | Output[Map[String, A]] | NotProvided)
  def asOutputMap(using ctx: Context): Output[Map[String, A]] =
    v match
      case NotProvided    => Output.empty
      case out: Output[_] => out.asInstanceOf[Output[Map[String, A]]] // TODO TypeTest?
      case m: Map[_, _] @unchecked =>
        if m.exists((_, v) => v.isInstanceOf[Output[_]]) then Output.traverseMap(m.asInstanceOf[Map[String, Output[A]]])
        else Output(m.asInstanceOf[Map[String, A]])
