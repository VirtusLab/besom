package besom.internal

import besom.types.*

case class EmptyArgs() derives ArgsEncoder

case class StackReferenceArgs(name: Output[NonEmptyString]) derives ArgsEncoder

object StackReferenceArgs:
  def apply(name: Input[NonEmptyString])(using Context): StackReferenceArgs =
    StackReferenceArgs(name.asOutput(false))
