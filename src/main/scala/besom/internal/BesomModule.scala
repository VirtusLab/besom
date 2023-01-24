package besom.internal

import scala.util.{Try, Either, Left, Right, Success, Failure}

trait BesomModule:
  type Eff[+A]

  type Outputs = Map[String, Output[Any]]

  object Output extends OutputFactory

  def run(program: Context ?=> Output[Outputs]): Eff[Unit] = ???

  def exports(outputs: (String, Output[Any])*): Output[Map[String, Output[Any]]] = ???
