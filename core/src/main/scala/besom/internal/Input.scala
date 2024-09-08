package besom.internal

opaque type Input[+A] >: A | Output[A] = A | Output[A]
import scala.collection.immutable.Iterable

object Input:
  opaque type Optional[+A] >: Input[A | Option[A]]                      = Input[A | Option[A]]
  opaque type OneOrIterable[+A] >: Input[A] | Input[Iterable[Input[A]]] = Input[A] | Input[Iterable[Input[A]]]

  extension [A](optional: A | Option[A])
    private def asOption: Option[A] =
      optional match
        case option: Option[A @unchecked] => option
        case a: A @unchecked              => Option(a)

  given simpleInputOps: {} with
    extension [A](input: Input[A])
      private[Input] def wrappedAsOutput(isSecret: Boolean = false): Output[A] =
        input match
          case output: Output[_] => output.asInstanceOf[Output[A]]
          case a: A @unchecked   => if isSecret then Output.secret(a) else Output.pure(a)

    extension [A](input: Input[A])
      def asOutput(isSecret: Boolean = false): Output[A] =
        input.wrappedAsOutput(isSecret)

    extension [A](input: Input.Optional[A])
      private[Input] def wrappedAsOptionOutput(isSecret: Boolean = false): Output[Option[A]] =
        val output = input match
          case output: Output[_]                  => output.asInstanceOf[Output[A | Option[A]]]
          case maybeA: (A | Option[A]) @unchecked => if isSecret then Output.secret(maybeA) else Output.pure(maybeA)
        output.map(_.asOption)

      def asOptionOutput(isSecret: Boolean = false): Output[Option[A]] =
        input.wrappedAsOptionOutput(isSecret)

  given mapInputOps: {} with
    private def inputMapToMapOutput[A](inputMap: Map[String, Input[A]], isSecret: Boolean): Output[Map[String, A]] =
      val outputMap = inputMap.view.mapValues(_.asOutput(isSecret)).toMap
      Output.traverseMap(outputMap)

    extension [A](input: Input[Map[String, Input[A]]])
      def asOutput(isSecret: Boolean = false): Output[Map[String, A]] =
        input.wrappedAsOutput(isSecret).flatMap(inputMapToMapOutput(_, isSecret = isSecret))

    extension [A](input: Input.Optional[Map[String, Input[A]]])
      def asOptionOutput(isSecret: Boolean = false): Output[Option[Map[String, A]]] =
        input.wrappedAsOptionOutput(isSecret).flatMap {
          case Some(map) => inputMapToMapOutput(map, isSecret = isSecret).map(Option(_))
          case None      => Output.pure(None)
        }

  given iterableInputOps: {} with
    private def inputIterableToIterableOutput[A](inputIterable: Iterable[Input[A]], isSecret: Boolean): Output[Iterable[A]] =
      val outputIterable = inputIterable.map(simpleInputOps.asOutput(_)(isSecret = isSecret))
      Output.sequence(outputIterable)

    extension [A](input: Input.OneOrIterable[A])
      def asManyOutput(isSecret: Boolean = false): Output[Iterable[A]] =
        input.wrappedAsOutput(isSecret).flatMap {
          case iterable: Iterable[Input[A]] @unchecked => inputIterableToIterableOutput(iterable, isSecret = isSecret)
          case a: A @unchecked                         => Output.pure(Iterable(a))
        }

    extension [A](input: Input[Iterable[Input[A]]])
      def asOutput(isSecret: Boolean = false): Output[Iterable[A]] =
        input.wrappedAsOutput(isSecret).flatMap(inputIterableToIterableOutput(_, isSecret = isSecret))

    extension [A](input: Input.Optional[Iterable[Input[A]]])
      def asOptionOutput(isSecret: Boolean = false): Output[Option[Iterable[A]]] =
        input.wrappedAsOptionOutput(isSecret).flatMap {
          case Some(map) => inputIterableToIterableOutput(map, isSecret = isSecret).map(Option(_))
          case None      => Output.pure(None)
        }
end Input
