package besom.internal

import besom.util.*
import RunResult.{given, *}
import scala.collection.immutable.Iterable

class OutputTest extends munit.FunSuite:

  def takesNEString(nestring: Input.Optional[NonEmptyString])(using
    Context
  ): Output[Option[NonEmptyString]] = nestring.asOptionOutput()

  def takesAList(list: Input[List[Input[String]]])(using
    Context
  ): Output[Iterable[String]] =
    list.asOutput()

  def takesAnOptionalList(list: Input.Optional[List[Input[String]]])(using
    Context
  ): Output[Option[Iterable[String]]] =
    list.asOptionOutput()

  def takesAMap(map: Input[Map[String, Input[String]]])(using
    Context
  ): Output[Map[String, String]] =
    map.asOutput()

  def takesAnOptionalMap(map: Input.Optional[Map[String, Input[String]]])(using
    Context
  ): Output[Option[Map[String, String]]] =
    map.asOptionOutput()

  def takesManyStrings(strings: Input.OneOrIterable[String])(using
    Context
  ): Output[Iterable[String]] =
    strings.asManyOutput()

  test("multi-input type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesNEString("string").getData.unsafeRunSync(), OutputData(Option("string")))
    assertEquals(takesNEString(Output.pure("string")).getData.unsafeRunSync(), OutputData(Option("string")))
    assertEquals(takesNEString(None).getData.unsafeRunSync(), OutputData(None))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multi-input many type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesManyStrings("value").getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(List("value")).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(List("value")).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(List(Output.pure("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(Output.pure(List("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(Output.pure(List(Output.pure("value")))).getData.unsafeRunSync(), OutputData(List("value")))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multi-input list type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesAList(List("value")).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesAList(List(Output.pure("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesAList(Output.pure(List("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesAList(Output.pure(List(Output.pure("value")))).getData.unsafeRunSync(), OutputData(List("value")))

    assertEquals(takesAnOptionalList(List("value")).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(List(Output.pure("value"))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Output.pure(List("value"))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Output.pure(List(Output.pure("value")))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Option(List("value"))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Option(List(Output.pure("value")))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Output.pure(Option(List("value")))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(
      takesAnOptionalList(Output.pure(Option(List(Output.pure("value"))))).getData.unsafeRunSync(),
      OutputData(Option(List("value")))
    )

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multi-input map type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesAMap(Map("key" -> "value")).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))
    assertEquals(takesAMap(Map("key" -> Output.pure("value"))).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))
    assertEquals(takesAMap(Output.pure(Map("key" -> "value"))).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))
    assertEquals(takesAMap(Output.pure(Map("key" -> Output.pure("value")))).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))

    assertEquals(takesAnOptionalMap(Map("key" -> "value")).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(takesAnOptionalMap(Map("key" -> Output.pure("value"))).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(takesAnOptionalMap(Output.pure(Map("key" -> "value"))).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(
      takesAnOptionalMap(Output.pure(Map("key" -> Output.pure("value")))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )
    assertEquals(takesAnOptionalMap(Option(Map("key" -> "value"))).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(
      takesAnOptionalMap(Option(Map("key" -> Output.pure("value")))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )
    assertEquals(
      takesAnOptionalMap(Output.pure(Option(Map("key" -> "value")))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )
    assertEquals(
      takesAnOptionalMap(Output.pure(Option(Map("key" -> Output.pure("value"))))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multiple evaluations of sequence") {
    given Context = DummyContext().unsafeRunSync()

    val seq = Output.sequence(List(Output.pure("value"), Output.pure("value2")))

    val firstEval = seq.getData.unsafeRunSync()
    assertEquals(firstEval, OutputData(List("value", "value2")))

    val secondEval = seq.getData.unsafeRunSync()
    assertEquals(secondEval, OutputData(List("value", "value2")))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multiple evaluations of parSequence") {
    given Context = DummyContext().unsafeRunSync()

    val seq = Output.parSequence(List(Output.pure("value"), Output.pure("value2")))

    val firstEval = seq.getData.unsafeRunSync()
    assertEquals(firstEval, OutputData(List("value", "value2")))

    val secondEval = seq.getData.unsafeRunSync()
    assertEquals(secondEval, OutputData(List("value", "value2")))

    Context().waitForAllTasks.unsafeRunSync()
  }

  // also not very proud of this test
  test("parSequence works in parallel") {
    given Context = DummyContext().unsafeRunSync()

    def sleepAndReturn[A](value: A): Output[A] =
      Output.ofResult(Result.sleep(50).map(_ => value))

    def timeOutput(output: Output[?]): Output[Long] =
      Output.ofResult(Result.defer(System.currentTimeMillis())).zip(output).map { case (start, _) => System.currentTimeMillis() - start }

    val time = timeOutput(Output.parSequence(List(sleepAndReturn("value"), sleepAndReturn("value2"), sleepAndReturn("value3"))))

    val firstEval = time.getData.unsafeRunSync()
    assert(firstEval.getValue.getOrElse { throw Exception("Expected a value") } < 70) // 20ms of leeway

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multiple evaluations of traverse") {
    given Context = DummyContext().unsafeRunSync()

    import besom.aliases.OutputExtensions.*
    val out: Output[List[String]] = List("value", "value2").traverse(Output.pure(_))

    val firstEval = out.getData.unsafeRunSync()
    assertEquals(firstEval, OutputData(List("value", "value2")))

    val secondEval = out.getData.unsafeRunSync()
    assertEquals(secondEval, OutputData(List("value", "value2")))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("Output.sequence works with all kinds of collections") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(
      Output.sequence(List(Output.pure("value"), Output.pure("value2"))).getData.unsafeRunSync(),
      OutputData(List("value", "value2"))
    )
    assertEquals(
      Output.sequence(Vector(Output.pure("value"), Output.pure("value2"))).getData.unsafeRunSync(),
      OutputData(Vector("value", "value2"))
    )
    assertEquals(
      Output.sequence(Set(Output.pure("value"), Output.pure("value2"))).getData.unsafeRunSync(),
      OutputData(Set("value", "value2"))
    )
    assertEquals(
      Output.sequence(Array(Output.pure("value"), Output.pure("value2")).toList).getData.unsafeRunSync(),
      OutputData(List("value", "value2"))
    )
    val iter: Iterable[String] = List("value", "value2")
    assertEquals(Output.sequence(iter.map(Output.pure(_))).getData.unsafeRunSync(), OutputData(List("value", "value2")))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("extensions for sequence and traverse work will all kinds of collections") {
    import besom.* // test global import
    given Context = DummyContext().unsafeRunSync()

    assertEquals(List(Output("value"), Output("value2")).sequence.getData.unsafeRunSync(), OutputData(List("value", "value2")))
    assertEquals(
      Vector(Output("value"), Output("value2")).sequence.getData.unsafeRunSync(),
      OutputData(Vector("value", "value2"))
    )
    assertEquals(Set(Output("value"), Output("value2")).sequence.getData.unsafeRunSync(), OutputData(Set("value", "value2")))
    assertEquals(
      Array("value", "value2").toList.traverse(x => Output(x)).getData.unsafeRunSync(),
      OutputData(List("value", "value2"))
    )

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("issue 430") {
    import java.io.File
    import besom.*
    object s3:
      def BucketObject(name: NonEmptyString)(using Context): Output[Unit] = Output(())

    given Context = DummyContext().unsafeRunSync()

    val uploads = File(".").listFiles().toList.traverse { file =>
      val name = NonEmptyString(file.getName) match
        case Some(name) => Output(name)
        case None       => Output(None).map(_ => throw new RuntimeException("Unexpected empty file name"))

      name.flatMap {
        s3.BucketObject(
          _
        )
      }
    }

    uploads.getData.unsafeRunSync()

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("Output#flatMap on non supported datatypes have nice and informative compile error") {
    val shouldCompile = scala.compiletime.testing.typeCheckErrors(
      """import besom.*
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()

         val out: Output[Int] = Output.pure(1).flatMap(x => Output.pure(x + 1))"""
    )

    assert(shouldCompile.isEmpty)

    val shouldNotCompile = scala.compiletime.testing.typeCheckErrors(
      """import besom.*
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()

         val out: Output[Int] = Output.pure(1).flatMap(x => Option(x + 1))"""
    )

    assert(shouldNotCompile.size == 1)

    val expected = """Could not find a given ToFuture instance for type Option.
                     |
                     |Besom offers the following instances:
                     |  * besom-core provides a ToFuture instance for scala.concurrent.Future
                     |  * besom-zio provides a ToFuture instance for zio.Task
                     |  * besom-cats provides a ToFuture instance for cats.effect.IO""".stripMargin

    assertEquals(shouldNotCompile.head.message, expected)

    val shouldNotCompileFlatMapRawValue = scala.compiletime.testing.typeCheckErrors(
      """import besom.*
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()

         val out: Output[Int] = Output.pure(1).flatMap(_ => 1)"""
    )

    assert(shouldNotCompileFlatMapRawValue.size == 1)

    val expectedFlatMapRawValue =
      """Output#flatMap can only be used with functions that return an Output or a structure like scala.concurrent.Future, cats.effect.IO or zio.Task.
        |If you want to map over the value of an Output, use the map method instead.""".stripMargin

    assertEquals(shouldNotCompileFlatMapRawValue.head.message, expectedFlatMapRawValue)
  }

  Vector(
    (true, "value", Some("value")),
    (false, "value", None)
  ).foreach { (cond, value, expected) =>
    given Context = DummyContext().unsafeRunSync()
    for
      outCond <- Vector(true, false)
      optVal  <- Vector(true, false)
      outVal  <- Vector(true, false)
    do
      val c = if outCond then Output.pure(cond) else cond
      val v = (outVal, optVal) match
        case (true, true)   => Output.pure(Option(value))
        case (true, false)  => Output.pure(value)
        case (false, true)  => Some(value)
        case (false, false) => value

      test(s"Output.when ${cond} then ${value} (outCond: ${outCond}, valOpt: ${optVal}, valOut: ${outVal})") {
        val result = Output.when(c)(v)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("Option.getOrFail get value") {
    import besom.OutputOptionOps
    given Context = DummyContext().unsafeRunSync()

    val result = Output.pure(Some("value")).getOrFail(Exception("error"))
    assertEquals(result.getData.unsafeRunSync(), OutputData("value"))
  }

  test("Option.getOrFail throws") {
    import besom.OutputOptionOps
    given Context = DummyContext().unsafeRunSync()

    val result = Output.pure(None).getOrFail(Exception("error"))
    interceptMessage[Exception]("error")(result.getData.unsafeRunSync())
  }

  Vector(
    (Some("value"), "default", "value"),
    (None, "default", "default")
  ).foreach { (value, default, expected) =>
    import besom.OutputOptionOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      val d = outVal match
        case true  => Output.pure(default)
        case false => default

      test(s"Option.getOrElse ${value} or ${default} (outVal: ${outVal})") {
        val result = Output.pure(value).getOrElse(d)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }

  Vector(
    (Some("value"), Some("default"), Some("value")),
    (None, Some("default"), Some("default")),
    (Some("value"), None, Some("value")),
    (None, None, None)
  ).foreach { (value, default, expected) =>
    import besom.OutputOptionOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      val d = outVal match
        case true  => Output.pure(default)
        case false => default

      test(s"Option.orElse ${value} orElse ${default} (outVal: ${outVal})") {
        val result = Output.pure(value).orElse(d)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }

  Vector(
    (Some("value"), (v: String) => v.toUpperCase, Some("VALUE")),
    (None, (v: String) => v.toUpperCase, None)
  ).foreach { (value, f, expected) =>
    import besom.OutputOptionOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      test(s"Option.mapInner ${value} (outVal: ${outVal})") {
        val result: Output[Option[String]] = // FIXME: the inference is not working without the explicit type
          if outVal then Output.pure(value).mapInner(f.andThen(Output.pure(_)))
          else Output.pure(value).mapInner(f)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }

  Vector(
    (Some("value"), (v: String) => Some(v.toUpperCase), Some("VALUE")),
    (None, (v: String) => Some(v.toUpperCase), None)
  ).foreach { (value, f, expected) =>
    import besom.OutputOptionOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      test(s"Option.flatMapInner ${value} (outVal: ${outVal})") {
        val result =
          if outVal then Output.pure(value).flatMapInner(f.andThen(Output.pure(_)))
          else Output.pure(value).flatMapInner(f)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }

  Vector(
    (List.empty[String], None),
    (List("value"), Some("value")),
    (List("value", "value2"), Some("value"))
  ).foreach { (value, expected) =>
    import besom.OutputListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"List.headOption ${value}") {
      val result = Output.pure(value).headOption
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (List.empty[String], None),
    (List("value"), Some("value")),
    (List("value", "value2"), Some("value2"))
  ).foreach { (value, expected) =>
    import besom.OutputListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"List.lastOption ${value}") {
      val result = Output.pure(value).lastOption
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (List.empty[String], List.empty[String]),
    (List("value"), List.empty[String]),
    (List("value", "value2"), List("value2")),
    (List("value", "value2", "value3"), List("value2", "value3"))
  ).foreach { (value, expected) =>
    import besom.OutputListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"List.tailOrEmpty ${value}") {
      val result = Output.pure(value).tailOrEmpty
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (List.empty[String], List.empty[String]),
    (List("value"), List.empty[String]),
    (List("value", "value2"), List("value")),
    (List("value", "value2", "value3"), List("value", "value2"))
  ).foreach { (value, expected) =>
    import besom.OutputListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"List.initOrEmpty ${value}") {
      val result = Output.pure(value).initOrEmpty
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (List.empty[String], (v: String) => v.toUpperCase, List.empty[String]),
    (List("value"), (v: String) => v.toUpperCase, List("VALUE")),
    (List("value", "value2"), (v: String) => v.toUpperCase, List("VALUE", "VALUE2"))
  ).foreach { (value, f, expected) =>
    import besom.OutputListOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      test(s"List.mapInner ${value}") {
        val result =
          if outVal then Output.pure(value).mapInner(f.andThen(Output.pure(_)))
          else Output.pure(value).mapInner(f)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }

  Vector(
    (List.empty[String], (v: String) => v.toUpperCase.iterator.map(_.toString).toList, List.empty[String]),
    (List("value"), (v: String) => v.toUpperCase.iterator.map(_.toString).toList, List("V", "A", "L", "U", "E")),
    (
      List("value", "value2"),
      (v: String) => v.toUpperCase.iterator.map(_.toString).toList,
      List("V", "A", "L", "U", "E", "V", "A", "L", "U", "E", "2")
    )
  ).foreach { (value, f, expected) =>
    import besom.OutputListOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      test(s"List.flatMapInner ${value}") {
        val result =
          if outVal then Output.pure(value).flatMapInner(f.andThen(Output.pure(_)))
          else Output.pure(value).flatMapInner(f)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }

  Vector(
    (Some(List.empty[String]), None),
    (Some(List("value")), Some("value")),
    (Some(List("value", "value2")), Some("value")),
    (None, None)
  ).foreach { (value, expected) =>
    import besom.OutputOptionListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"Option[List].headOption ${value}") {
      val result = Output.pure(value).headOption
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (Some(List.empty[String]), None),
    (Some(List("value")), Some("value")),
    (Some(List("value", "value2")), Some("value2")),
    (None, None)
  ).foreach { (value, expected) =>
    import besom.OutputOptionListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"Option[List].lastOption ${value}") {
      val result = Output.pure(value).lastOption
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (Some(List.empty[String]), List.empty[String]),
    (Some(List("value")), List.empty[String]),
    (Some(List("value", "value2")), List("value2")),
    (Some(List("value", "value2", "value3")), List("value2", "value3")),
    (None, List.empty[String])
  ).foreach { (value, expected) =>
    import besom.OutputOptionListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"Option[List].tailOrEmpty ${value}") {
      val result = Output.pure(value).tailOrEmpty
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (Some(List.empty[String]), List.empty[String]),
    (Some(List("value")), List.empty[String]),
    (Some(List("value", "value2")), List("value")),
    (Some(List("value", "value2", "value3")), List("value", "value2")),
    (None, List.empty[String])
  ).foreach { (value, expected) =>
    import besom.OutputOptionListOps
    given Context = DummyContext().unsafeRunSync()

    test(s"Option[List].initOrEmpty ${value}") {
      val result = Output.pure(value).initOrEmpty
      assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
    }
  }

  Vector(
    (None, (v: String) => v.toUpperCase, List.empty[String]),
    (
      Some(List.empty[String]),
      (v: String) => v.toUpperCase,
      List.empty[String]
    ),
    (
      Some(List("value")),
      (v: String) => v.toUpperCase,
      List("VALUE")
    ),
    (
      Some(List("value", "value2")),
      (v: String) => v.toUpperCase,
      List("VALUE", "VALUE2")
    )
  ).foreach { (value, f, expected) =>
    import besom.OutputOptionListOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      test(s"Option[List].mapInner ${value}") {
        val result =
          if outVal then Output.pure(value).mapInner(f.andThen(Output.pure(_)))
          else Output.pure(value).mapInner(f)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }

  Vector(
    (None, (v: String) => v.toUpperCase.iterator.map(_.toString).toList, List.empty[String]),
    (
      Some(List.empty[String]),
      (v: String) => v.toUpperCase.iterator.map(_.toString).toList,
      List.empty[String]
    ),
    (
      Some(List("value")),
      (v: String) => v.toUpperCase.iterator.map(_.toString).toList,
      List("V", "A", "L", "U", "E")
    ),
    (
      Some(List("value", "value2")),
      (v: String) => v.toUpperCase.iterator.map(_.toString).toList,
      List("V", "A", "L", "U", "E", "V", "A", "L", "U", "E", "2")
    )
  ).foreach { (value, f, expected) =>
    import besom.OutputOptionListOps
    given Context = DummyContext().unsafeRunSync()

    for outVal <- Vector(true, false)
    do
      test(s"Option[List].flatMapInner ${value}") {
        val result =
          if outVal then Output.pure(value).flatMapInner(f.andThen(Output.pure(_)))
          else Output.pure(value).flatMapInner(f)
        assertEquals(result.getData.unsafeRunSync(), OutputData(expected))
      }
  }
  

  test("unzip combinator is able to unzip an Output of a tuple into a tuple of Outputs") {
    object extensions extends OutputExtensionsFactory
    import extensions.*

    given Context = DummyContext().unsafeRunSync()

    val o3 = Output.pure(("string", 23, true))

    val (str, int, bool) = o3.unzip

    assertEquals(str.getData.unsafeRunSync(), OutputData("string"))
    assertEquals(int.getData.unsafeRunSync(), OutputData(23))
    assertEquals(bool.getData.unsafeRunSync(), OutputData(true))

    // explicitly tuple of 20 elements
    val tupleOf22Elems = (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)

    val o22 = Output.pure(tupleOf22Elems)

    val tupleOf22Outputs = o22.unzip

    assertEquals(tupleOf22Outputs.size, 22)

    // explicitly tuple of 23 elements, testing tuple xxl
    val tupleOf23Elems = (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, "23")

    val o23 = Output.pure(tupleOf23Elems)

    val tupleOf23Outputs = o23.unzip

    assertEquals(tupleOf23Outputs.size, 23)

    tupleOf23Outputs.toArray.map(_.asInstanceOf[Output[Int | String]]).zipWithIndex.foreach { (output, idx) =>
      if idx == 22 then assertEquals(output.getData.unsafeRunSync(), OutputData("23"))
      else assertEquals(output.getData.unsafeRunSync(), OutputData(idx + 1))
    }
  }

  test("recover combinator is able to recover from a failed Output") {
    given Context = DummyContext().unsafeRunSync()

    val failedOutput: Output[Int] = Output.fail(Exception("error"))

    val recoveredOutput = failedOutput.recover { case _: Exception =>
      42
    }

    assertEquals(recoveredOutput.getData.unsafeRunSync(), OutputData(42))
  }

  test("recoverWith combinator is able to recover from a failed Output with another Output") {
    given Context = DummyContext().unsafeRunSync()

    val failedOutput: Output[Int] = Output.fail(Exception("error"))

    val recoveredOutput = failedOutput.recoverWith { case _: Exception =>
      Output.pure(42)
    }

    assertEquals(recoveredOutput.getData.unsafeRunSync(), OutputData(42))
  }

  test("recoverWith combinator is able to subsume an effect like flatMap") {
    import scala.concurrent.Future
    import besom.*

    given Context = DummyContext().unsafeRunSync()

    val failedOutput: Output[Int] = Output.fail(Exception("error"))

    val recoveredOutput = failedOutput.recoverWith { case _: Exception =>
      Future.successful(42)
    }

    assertEquals(recoveredOutput.getData.unsafeRunSync(), OutputData(42))
  }

  test("tap combinator is able to tap into the value of an Output") {
    object Output extends OutputFactory

    given Context = DummyContext().unsafeRunSync()

    var tappedValue = 0

    val output = Output(42).tap { value =>
      tappedValue = value
      Output.unit
    }

    assertEquals(output.getData.unsafeRunSync(), OutputData(42))
    assertEquals(tappedValue, 42)
  }

  test("tapError combinator is able to tap into the error of a failed Output") {
    object Output extends OutputFactory

    given Context = DummyContext().unsafeRunSync()

    var tappedError: Throwable = new RuntimeException("everything is fine")

    val failedOutput = Output.fail(new RuntimeException("error")).tapError { error =>
      tappedError = error
      Output.unit
    }

    interceptMessage[RuntimeException]("error")(failedOutput.getData.unsafeRunSync())
    assertEquals(tappedError.getMessage, "error")
  }

  test("tapBoth combinator is able to tap into the value and error of an Output") {
    object Output extends OutputFactory

    given Context = DummyContext().unsafeRunSync()

    var tappedValue            = 0
    var tappedError: Throwable = new RuntimeException("everything is fine")

    val output = Output(42)
      .tapBoth(
        value => {
          tappedValue = value
          Output.unit
        },
        error => {
          tappedError = error
          Output.unit
        }
      )

    assertEquals(output.getData.unsafeRunSync(), OutputData(42))
    assertEquals(tappedValue, 42)
    assertEquals(tappedError.getMessage, "everything is fine")

    tappedValue = 0

    val failedOutput = Output
      .fail(new RuntimeException("error"))
      .tapBoth(
        value => {
          tappedValue = value
          Output.unit
        },
        error => {
          tappedError = error
          Output.unit
        }
      )

    interceptMessage[RuntimeException]("error")(failedOutput.getData.unsafeRunSync())
    assertEquals(tappedValue, 0)
    assertEquals(tappedError.getMessage, "error")
  }

end OutputTest
