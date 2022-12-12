package besom.internal

import scala.util.{Try, Either, Left, Right, Success, Failure}

trait BesomModule:
  type F[+A]
  given F: Monad[F]

  type Output[A] = besom.internal.Output[F, A]
  type Context   = besom.internal.Context[F]

  type Outputs = Map[String, Output[Any]]

  object Output extends OutputFactory[F]

  def run(program: Context ?=> Output[Outputs]): F[Unit] = ???
