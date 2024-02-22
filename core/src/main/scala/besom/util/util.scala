package besom.util

import besom.{Context, Output}

//noinspection ScalaFileName
given eitherOps: {} with

  extension [E <: Throwable, A](a: Either[E, A])
    def get: A                             = a.fold(e => throw e, identity)
    def asOutput(using Context): Output[A] = a.fold(e => Output.fail(e), Output(_))
