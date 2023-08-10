package besom.internal

opaque type Input[+A] >: A | Output[A] = A | Output[A]

object Input:
  extension [A](input: Input[A])
    def asOutput(isSecret: Boolean = false)(using ctx: Context): Output[A] =
      input match
        case output: Output[_]  => output.asInstanceOf[Output[A]] // TODO TypeTest?
        case a: A @unchecked => if isSecret then Output.secret(a) else Output(a)


opaque type Optional[+A] >: A | Option[A] = A | Option[A]

object Optional:
  extension [A](optional: Optional[A])
    def asOption: Option[A] =
      optional match
        case option: Option[A @ unchecked] => option
        case a: A @unchecked => Option(a)



  // trait AsOutput[A]:
  //   type Result
  //   def apply(input: Input[A], isSecret: Boolean)(using Context): Output[Result]

  // object AsOutput /* extends AsOutputLowPrio */:
  //   // given mapAsOutput[T](using innerAsOutput: AsOutput[T]): AsOutput[Map[String, Input[T]]] with
  //   //   type Result = Map[String, innerAsOutput.Result]
  //   //   def apply(input: Input[Map[String, Input[T]]], isSecret: Boolean)(using Context): Output[Result] =
  //   //     input match
  //   //       case output: Output[_] => output.asInstanceOf[Output[Result]]
  //   //       case map: Map[String, Input[T]] @unchecked => Output.traverseMap[innerAsOutput.Result](map.map { (key, value) => (key, innerAsOutput(value, isSecret = isSecret)) })

  //   given mapAsOutput[T](using innerAsOutput: AsOutput[T]): (AsOutput[Map[String, Input[T]]] { type Result = Map[String, innerAsOutput.Result] }) = new AsOutput:
  //     type Result = Map[String, innerAsOutput.Result]
  //     def apply(input: Input[Map[String, Input[T]]], isSecret: Boolean)(using Context): Output[Result] =
  //       input match
  //         case output: Output[_] => output.asInstanceOf[Output[Result]]
  //         case map: Map[String, Input[T]] @unchecked => Output.traverseMap[innerAsOutput.Result](map.map { (key, value) => (key, innerAsOutput(value, isSecret = isSecret)) })


  //   given listAsOutput[T](using innerAsOutput: AsOutput[T]): AsOutput[List[Input[T]]] with
  //     type Result = List[innerAsOutput.Result]
  //     def apply(input: Input[List[Input[T]]], isSecret: Boolean)(using Context): Output[Result] =
  //       input match
  //         case output: Output[_] => output.asInstanceOf[Output[Result]]
  //         case list: List[Input[T]] @unchecked =>
  //           // val list1: List[Output[innerAsOutput.Result]] = list.map { value => innerAsOutput(value, isSecret = isSecret) }
  //           Output.sequence(
  //             list.map { value => innerAsOutput(value, isSecret = isSecret) }
  //           )

  //   given optionAsOutput[T](using innerAsOutput: AsOutput[T]): AsOutput[Option[T]] with
  //     type Result = Option[innerAsOutput.Result]
  //     def apply(input: Input[Option[T]], isSecret: Boolean)(using Context): Output[Result] =
  //       input match
  //         case output: Output[_] => output.asInstanceOf[Output[Result]]
  //         case option: Option[Input[T]] @unchecked =>
  //           // option.map(value => innerAsOutput(value, isSecret = isSecret)) match
  //           option match
  //             case None => Output(None)
  //             case Some(value) => innerAsOutput(value, isSecret = isSecret).map(Option(_)) //Output(Option(innerAsOutput(value, isSecret = isSecret))) //Option(innerAsOutput(value, isSecret = isSecret))

  // // trait AsOutputLowPrio:
  //   given valueAsOutput[T]: AsOutput[T] with
  //     type Result = T
  //     def apply(input: Input[T], isSecret: Boolean)(using Context): Output[Result] =
  //       input match
  //         case output: Output[_] => output.asInstanceOf[Output[Result]]
  //         case value: T @unchecked => if isSecret then Output.secret(value) else Output(value)

  //   trait Foo

  //   // val ao: AsOutput[Option[scala.Predef.Map[String, Input[Foo]]]] = optionAsOutput[Map[String, Input[Foo]]]
  //   // val ao: AsOutput[Option[scala.Predef.Map[String, Input[Foo]]]] = summon[Input[Option[scala.Predef.Map[String, Input[Foo]]]]]

  //   // val input1: Input[Option[scala.Predef.Map[String, Input[Foo]]]] = Option(Map("cds" -> new Foo {}))
  //   // val abc: Output[Option[scala.Predef.Map[String, Output[Foo]]]] = ao(input1, isSecret = false)(using ???)

  //   val ao: AsOutput[Option[scala.Predef.Map[String, Input[Int]]]] = optionAsOutput[Map[String, Input[Int]]](using mapAsOutput[Input[Int]])
  //   val input1: Input[Option[scala.Predef.Map[String, Input[Int]]]] = Option(Map("cds" -> 1))
  //   val abc: Output[Option[scala.Predef.Map[String, Output[Int]]]] = ao(input1, isSecret = false)(using ???)


  // extension [A](input: Input[A])
  //   def asOutput(isSecret: Boolean = false)(using ctx: Context, ao: AsOutput[A]): Output[ao.Result] = ao.apply(input, isSecret = isSecret)


  // // extension [A](input: Input[A])
  // //   def asOutput(isSecret: Boolean = false)(using ctx: Context): Output[A] =
  // //     input match
  // //       case output: Output[_]  => output.asInstanceOf[Output[A]] // TODO TypeTest?
  // //       case list: List[_]
  // //       case a: A @unchecked => if isSecret then Output.secret(a) else Output(a)