package besom.internal

import besom.util.NonEmptyString

case class StackReferenceArgs(name: Output[NonEmptyString]) derives ArgsEncoder

trait StackReferenceArgsFactory:
  def apply(name: Input[NonEmptyString])(using Context): StackReferenceArgs =
    StackReferenceArgs(name.asOutput(false))
