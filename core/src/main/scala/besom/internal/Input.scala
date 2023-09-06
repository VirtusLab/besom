package besom.internal

opaque type Input[+A] >: A | Output[A] = A | Output[A]

object Input:
  opaque type Optional[+A] >: Input[A | Option[A]] = Input[A | Option[A]]

  extension [A](optional: A | Option[A])
    private def asOption: Option[A] =
      optional match
        case option: Option[A @ unchecked] => option
        case a: A @unchecked => Option(a)

  given simpleInputOps: {} with
    extension [A](input: Input[A])
      private[Input] def wrappedAsOutput(isSecret: Boolean = false)(using ctx: Context): Output[A] =
        input match
          case output: Output[_] => output.asInstanceOf[Output[A]]
          case a: A @unchecked => if isSecret then Output.secret(a) else Output(a)

    extension [A](input: Input[A])
      def asOutput(isSecret: Boolean = false)(using ctx: Context): Output[A] =
        input.wrappedAsOutput(isSecret)

    extension [A](input: Input.Optional[A])
      private[Input] def wrappedAsOptionOutput(isSecret: Boolean = false)(using ctx: Context): Output[Option[A]] =
        val output = input match
          case output: Output[_] => output.asInstanceOf[Output[A | Option[A]]]
          case maybeA: (A | Option[A]) @unchecked => if isSecret then Output.secret(maybeA) else Output(maybeA)
        output.map(_.asOption)

      def asOptionOutput(isSecret: Boolean = false)(using ctx: Context): Output[Option[A]] =
        input.wrappedAsOptionOutput(isSecret)

  given mapInputOps: {} with
    private def inputMapToMapOutput[A](inputMap: Map[String, Input[A]], isSecret: Boolean)(using ctx: Context): Output[Map[String, A]] =
      val outputMap = inputMap.mapValues(_.asOutput(isSecret)).toMap
      Output.traverseMap(outputMap)

    extension [A](input: Input[Map[String, Input[A]]])
      def asOutput(isSecret: Boolean = false)(using ctx: Context): Output[Map[String, A]] =
        input.wrappedAsOutput(isSecret).flatMap(inputMapToMapOutput(_, isSecret = isSecret))


    extension [A](input: Input.Optional[Map[String, Input[A]]])
      def asOptionOutput(isSecret: Boolean = false)(using ctx: Context): Output[Option[Map[String, A]]] =
        input.wrappedAsOptionOutput(isSecret).flatMap { 
          case Some(map) => inputMapToMapOutput(map, isSecret = isSecret).map(Option(_))
          case None => Output(None)
        }

  given listInputOps: {} with
    private def inputListToListOutput[A](inputList: List[Input[A]], isSecret: Boolean)(using ctx: Context): Output[List[A]] =
      val outputList = inputList.map(simpleInputOps.asOutput(_)(isSecret = isSecret))
      Output.sequence(outputList)

    extension [A](input: Input[List[Input[A]]])
      def asOutput(isSecret: Boolean = false)(using ctx: Context): Output[List[A]] =
        input.wrappedAsOutput(isSecret).flatMap(inputListToListOutput(_, isSecret = isSecret))

    extension [A](input: Input.Optional[List[Input[A]]])
      def asOptionOutput(isSecret: Boolean = false)(using ctx: Context): Output[Option[List[A]]] =
        input.wrappedAsOptionOutput(isSecret).flatMap { 
          case Some(map) => inputListToListOutput(map, isSecret = isSecret).map(Option(_))
          case None => Output(None)
        }
  