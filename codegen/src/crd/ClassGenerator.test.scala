package besom.codegen.crd

import besom.codegen.*

import scala.meta.*
import scala.meta.dialects.Scala33

class ClassGeneratorTest extends munit.FunSuite {
  case class Data(
    name: String,
    yaml: String,
    ignored: List[String] = List.empty,
    expected: Map[String, String] = Map.empty,
    expectedError: Option[String] = None,
    tags: Set[munit.Tag] = Set()
  )

  Vector(
    Data(
      name = "Simple CronTab definition",
      yaml = """apiVersion: apiextensions.k8s.io/v1
          |kind: CustomResourceDefinition
          |metadata:
          |  name: crontabs.stable.example.com
          |spec:
          |  group: stable.example.com
          |  versions:
          |    - name: v1
          |      schema:
          |        openAPIV3Schema:
          |          type: object
          |          properties:
          |            spec:
          |              type: object
          |              properties:
          |                cronSpec:
          |                  type: string
          |                image:
          |                  type: string
          |                replicas:
          |                  type: integer
          |  names:
          |    plural: crontabs
          |    singular: crontab
          |    kind: CronTab
          |    shortNames:
          |    - ct
          |""".stripMargin,
      expected = Map(
        "crontab/v1/CronTab.scala" ->
          """package crontab.v1
          |
          |final case class CronTab private(
          |  cronSpec: besom.types.Output[scala.Option[String]],
          |  image: besom.types.Output[scala.Option[String]],
          |  replicas: besom.types.Output[scala.Option[Int]],
          |) derives besom.Decoder, besom.Encoder
          |
          |object CronTab:
          |  def apply(
          |    cronSpec: besom.types.Input.Optional[String] = scala.None,
          |    image: besom.types.Input.Optional[String] = scala.None,
          |    replicas: besom.types.Input.Optional[Int] = scala.None
          |  )(using besom.types.Context): CronTab =
          |    new CronTab(
          |      cronSpec = cronSpec.asOptionOutput(isSecret = false),
          |      image = image.asOptionOutput(isSecret = false),
          |      replicas = replicas.asOptionOutput(isSecret = false)
          |    )
          |
          |  extension (cls: CronTab) def withArgs(
          |    cronSpec: besom.types.Input.Optional[String] = cls.cronSpec,
          |    image: besom.types.Input.Optional[String] = cls.image,
          |    replicas: besom.types.Input.Optional[Int] = cls.replicas
          |  )(using besom.types.Context): CronTab =
          |    new CronTab(
          |      cronSpec = cronSpec.asOptionOutput(isSecret = false),
          |      image = image.asOptionOutput(isSecret = false),
          |      replicas = replicas.asOptionOutput(isSecret = false)
          |    )
          |
          |  given outputOps: {} with
          |    extension(output: besom.types.Output[CronTab])
          |      def cronSpec: besom.types.Output[scala.Option[String]] = output.flatMap(_.cronSpec)
          |      def image: besom.types.Output[scala.Option[String]] = output.flatMap(_.image)
          |      def replicas: besom.types.Output[scala.Option[Int]] = output.flatMap(_.replicas)
          |
          |  given optionOutputOps: {} with
          |    extension(output: besom.types.Output[scala.Option[CronTab]])
          |      def cronSpec: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.cronSpec)
          |      def image: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.image)
          |      def replicas: besom.types.Output[scala.Option[Int]] = output.flatMapOpt(_.replicas)
          |
          |
          |  extension [A](output: besom.types.Output[scala.Option[A]])
          |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
          |      output.flatMap(
          |        _.map(f)
          |          .getOrElse(output.map(_ => scala.None))
          |      )
          |
          |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
          |      flatMapOpt(f(_).map(Some(_)))
          |""".stripMargin
      )
    ),
    Data(
      name = "Nested array types with object type",
      yaml = """apiVersion: apiextensions.k8s.io/v1
          |kind: CustomResourceDefinition
          |spec:
          |  group: example.com
          |  versions:
          |    - name: v1
          |      schema:
          |        openAPIV3Schema:
          |          type: object
          |          properties:
          |            spec:
          |              type: object
          |              properties:
          |                foo:
          |                  type: array
          |                  items:
          |                    type: array
          |                    items:
          |                      type: object
          |                      properties:
          |                        bar:
          |                          type: string
          |  names:
          |    singular: test
          |    kind: Test
          |""".stripMargin,
      expected = Map(
        "test/v1/Foo.scala" ->
          """package test.v1
            |
            |final case class Foo private(
            |  bar: besom.types.Output[scala.Option[String]],
            |) derives besom.Decoder, besom.Encoder
            |
            |object Foo:
            |  def apply(
            |    bar: besom.types.Input.Optional[String] = scala.None
            |  )(using besom.types.Context): Foo =
            |    new Foo(
            |      bar = bar.asOptionOutput(isSecret = false)
            |    )
            |
            |  extension (cls: Foo) def withArgs(
            |    bar: besom.types.Input.Optional[String] = cls.bar
            |  )(using besom.types.Context): Foo =
            |    new Foo(
            |      bar = bar.asOptionOutput(isSecret = false)
            |    )
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[Foo])
            |      def bar: besom.types.Output[scala.Option[String]] = output.flatMap(_.bar)
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[Foo]])
            |      def bar: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.bar)
            |
            |
            |  extension [A](output: besom.types.Output[scala.Option[A]])
            |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
            |      output.flatMap(
            |        _.map(f)
            |          .getOrElse(output.map(_ => scala.None))
            |      )
            |
            |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
            |      flatMapOpt(f(_).map(Some(_)))
            |""".stripMargin,
        "test/v1/Test.scala" ->
          """package test.v1
            |
            |final case class Test private(
            |  foo: besom.types.Output[scala.Option[scala.collection.immutable.Iterable[scala.collection.immutable.Iterable[test.v1.Foo]]]],
            |) derives besom.Decoder, besom.Encoder
            |
            |object Test:
            |  def apply(
            |    foo: besom.types.Input.Optional[scala.collection.immutable.Iterable[scala.collection.immutable.Iterable[test.v1.Foo]]] = scala.None
            |  )(using besom.types.Context): Test =
            |    new Test(
            |      foo = foo.asOptionOutput(isSecret = false)
            |    )
            |
            |  extension (cls: Test) def withArgs(
            |    foo: besom.types.Input.Optional[scala.collection.immutable.Iterable[scala.collection.immutable.Iterable[test.v1.Foo]]] = cls.foo
            |  )(using besom.types.Context): Test =
            |    new Test(
            |      foo = foo.asOptionOutput(isSecret = false)
            |    )
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[Test])
            |      def foo: besom.types.Output[scala.Option[scala.collection.immutable.Iterable[scala.collection.immutable.Iterable[test.v1.Foo]]]] = output.flatMap(_.foo)
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[Test]])
            |      def foo: besom.types.Output[scala.Option[scala.collection.immutable.Iterable[scala.collection.immutable.Iterable[test.v1.Foo]]]] = output.flatMapOpt(_.foo)
            |
            |
            |  extension [A](output: besom.types.Output[scala.Option[A]])
            |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
            |      output.flatMap(
            |        _.map(f)
            |          .getOrElse(output.map(_ => scala.None))
            |      )
            |
            |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
            |      flatMapOpt(f(_).map(Some(_)))
            |""".stripMargin
      )
    ),
    Data(
      name = "Numbers with boolean types",
      yaml = """apiVersion: apiextensions.k8s.io/v1
          |kind: CustomResourceDefinition
          |spec:
          |  group: example.com
          |  versions:
          |    - name: v1
          |      schema:
          |        openAPIV3Schema:
          |          type: object
          |          properties:
          |            spec:
          |              type: object
          |              properties:
          |                boolean:
          |                  type: boolean
          |                anyNumber:
          |                  type: number
          |                floatNumber:
          |                  type: number
          |                  format: float
          |                doubleNumber:
          |                  type: number
          |                  format: double
          |                anyInteger:
          |                  type: integer
          |                int32Integer:
          |                  type: integer
          |                  format: int32
          |                int64Integer:
          |                  type: integer
          |                  format: int64
          |  names:
          |    singular: number
          |    kind: Number
          |""".stripMargin,
      expected = Map(
        "number/v1/Number.scala" ->
          """package number.v1
            |
            |final case class Number private(
            |  anyInteger: besom.types.Output[scala.Option[Int]],
            |  boolean: besom.types.Output[scala.Option[Boolean]],
            |  doubleNumber: besom.types.Output[scala.Option[Double]],
            |  floatNumber: besom.types.Output[scala.Option[Float]],
            |  int32Integer: besom.types.Output[scala.Option[Int]],
            |  int64Integer: besom.types.Output[scala.Option[Long]],
            |  anyNumber: besom.types.Output[scala.Option[Double]],
            |) derives besom.Decoder, besom.Encoder
            |
            |object Number:
            |  def apply(
            |    anyInteger: besom.types.Input.Optional[Int] = scala.None,
            |    boolean: besom.types.Input.Optional[Boolean] = scala.None,
            |    doubleNumber: besom.types.Input.Optional[Double] = scala.None,
            |    floatNumber: besom.types.Input.Optional[Float] = scala.None,
            |    int32Integer: besom.types.Input.Optional[Int] = scala.None,
            |    int64Integer: besom.types.Input.Optional[Long] = scala.None,
            |    anyNumber: besom.types.Input.Optional[Double] = scala.None
            |  )(using besom.types.Context): Number =
            |    new Number(
            |      anyInteger = anyInteger.asOptionOutput(isSecret = false),
            |      boolean = boolean.asOptionOutput(isSecret = false),
            |      doubleNumber = doubleNumber.asOptionOutput(isSecret = false),
            |      floatNumber = floatNumber.asOptionOutput(isSecret = false),
            |      int32Integer = int32Integer.asOptionOutput(isSecret = false),
            |      int64Integer = int64Integer.asOptionOutput(isSecret = false),
            |      anyNumber = anyNumber.asOptionOutput(isSecret = false)
            |    )
            |
            |  extension (cls: Number) def withArgs(
            |    anyInteger: besom.types.Input.Optional[Int] = cls.anyInteger,
            |    boolean: besom.types.Input.Optional[Boolean] = cls.boolean,
            |    doubleNumber: besom.types.Input.Optional[Double] = cls.doubleNumber,
            |    floatNumber: besom.types.Input.Optional[Float] = cls.floatNumber,
            |    int32Integer: besom.types.Input.Optional[Int] = cls.int32Integer,
            |    int64Integer: besom.types.Input.Optional[Long] = cls.int64Integer,
            |    anyNumber: besom.types.Input.Optional[Double] = cls.anyNumber
            |  )(using besom.types.Context): Number =
            |    new Number(
            |      anyInteger = anyInteger.asOptionOutput(isSecret = false),
            |      boolean = boolean.asOptionOutput(isSecret = false),
            |      doubleNumber = doubleNumber.asOptionOutput(isSecret = false),
            |      floatNumber = floatNumber.asOptionOutput(isSecret = false),
            |      int32Integer = int32Integer.asOptionOutput(isSecret = false),
            |      int64Integer = int64Integer.asOptionOutput(isSecret = false),
            |      anyNumber = anyNumber.asOptionOutput(isSecret = false)
            |    )
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[Number])
            |      def anyInteger: besom.types.Output[scala.Option[Int]] = output.flatMap(_.anyInteger)
            |      def boolean: besom.types.Output[scala.Option[Boolean]] = output.flatMap(_.boolean)
            |      def doubleNumber: besom.types.Output[scala.Option[Double]] = output.flatMap(_.doubleNumber)
            |      def floatNumber: besom.types.Output[scala.Option[Float]] = output.flatMap(_.floatNumber)
            |      def int32Integer: besom.types.Output[scala.Option[Int]] = output.flatMap(_.int32Integer)
            |      def int64Integer: besom.types.Output[scala.Option[Long]] = output.flatMap(_.int64Integer)
            |      def anyNumber: besom.types.Output[scala.Option[Double]] = output.flatMap(_.anyNumber)
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[Number]])
            |      def anyInteger: besom.types.Output[scala.Option[Int]] = output.flatMapOpt(_.anyInteger)
            |      def boolean: besom.types.Output[scala.Option[Boolean]] = output.flatMapOpt(_.boolean)
            |      def doubleNumber: besom.types.Output[scala.Option[Double]] = output.flatMapOpt(_.doubleNumber)
            |      def floatNumber: besom.types.Output[scala.Option[Float]] = output.flatMapOpt(_.floatNumber)
            |      def int32Integer: besom.types.Output[scala.Option[Int]] = output.flatMapOpt(_.int32Integer)
            |      def int64Integer: besom.types.Output[scala.Option[Long]] = output.flatMapOpt(_.int64Integer)
            |      def anyNumber: besom.types.Output[scala.Option[Double]] = output.flatMapOpt(_.anyNumber)
            |
            |
            |  extension [A](output: besom.types.Output[scala.Option[A]])
            |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
            |      output.flatMap(
            |        _.map(f)
            |          .getOrElse(output.map(_ => scala.None))
            |      )
            |
            |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
            |      flatMapOpt(f(_).map(Some(_)))
            |
            |""".stripMargin
      )
    ),
    Data(
      name = "Base string types",
      yaml = """apiVersion: apiextensions.k8s.io/v1
          |kind: CustomResourceDefinition
          |spec:
          |  group: example.com
          |  versions:
          |    - name: v1
          |      schema:
          |        openAPIV3Schema:
          |          type: object
          |          properties:
          |            spec:
          |              type: object
          |              properties:
          |                date:
          |                  type: string
          |                  format: date
          |                dateTime:
          |                  type: string
          |                  format: date-time
          |                password:
          |                  type: string
          |                  format: password
          |                byte:
          |                  type: string
          |                  format: byte
          |                binary:
          |                  type: string
          |                  format: binary
          |  names:
          |    singular: string
          |    kind: String
          |""".stripMargin,
      expected = Map(
        "string/v1/String.scala" ->
          """package string.v1
            |
            |final case class String private(
            |  binary: besom.types.Output[scala.Option[String]],
            |  dateTime: besom.types.Output[scala.Option[java.time.LocalDateTime]],
            |  date: besom.types.Output[scala.Option[java.time.LocalDate]],
            |  byte: besom.types.Output[scala.Option[String]],
            |  password: besom.types.Output[scala.Option[String]],
            |) derives besom.Decoder, besom.Encoder
            |
            |object String:
            |  def apply(
            |    binary: besom.types.Input.Optional[String] = scala.None,
            |    dateTime: besom.types.Input.Optional[java.time.LocalDateTime] = scala.None,
            |    date: besom.types.Input.Optional[java.time.LocalDate] = scala.None,
            |    byte: besom.types.Input.Optional[String] = scala.None,
            |    password: besom.types.Input.Optional[String] = scala.None
            |  )(using besom.types.Context): String =
            |    new String(
            |      binary = binary.asOptionOutput(isSecret = false),
            |      dateTime = dateTime.asOptionOutput(isSecret = false),
            |      date = date.asOptionOutput(isSecret = false),
            |      byte = byte.asOptionOutput(isSecret = false),
            |      password = password.asOptionOutput(isSecret = true)
            |    )
            |
            |  extension (cls: String) def withArgs(
            |    binary: besom.types.Input.Optional[String] = cls.binary,
            |    dateTime: besom.types.Input.Optional[java.time.LocalDateTime] = cls.dateTime,
            |    date: besom.types.Input.Optional[java.time.LocalDate] = cls.date,
            |    byte: besom.types.Input.Optional[String] = cls.byte,
            |    password: besom.types.Input.Optional[String] = cls.password
            |  )(using besom.types.Context): String =
            |    new String(
            |      binary = binary.asOptionOutput(isSecret = false),
            |      dateTime = dateTime.asOptionOutput(isSecret = false),
            |      date = date.asOptionOutput(isSecret = false),
            |      byte = byte.asOptionOutput(isSecret = false),
            |      password = password.asOptionOutput(isSecret = true)
            |    )
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[String])
            |      def binary: besom.types.Output[scala.Option[String]] = output.flatMap(_.binary)
            |      def dateTime: besom.types.Output[scala.Option[java.time.LocalDateTime]] = output.flatMap(_.dateTime)
            |      def date: besom.types.Output[scala.Option[java.time.LocalDate]] = output.flatMap(_.date)
            |      def byte: besom.types.Output[scala.Option[String]] = output.flatMap(_.byte)
            |      def password: besom.types.Output[scala.Option[String]] = output.flatMap(_.password)
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[String]])
            |      def binary: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.binary)
            |      def dateTime: besom.types.Output[scala.Option[java.time.LocalDateTime]] = output.flatMapOpt(_.dateTime)
            |      def date: besom.types.Output[scala.Option[java.time.LocalDate]] = output.flatMapOpt(_.date)
            |      def byte: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.byte)
            |      def password: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.password)
            |
            |
            |  given besom.Encoder[java.time.LocalDateTime] with
            |    def encode(t: java.time.LocalDateTime)(using besom.Context): besom.internal.Result[(besom.internal.Metadata, com.google.protobuf.struct.Value)] =
            |      besom.internal.Encoder.stringEncoder.encode(t.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME))
            |
            |
            |  given besom.Decoder[java.time.LocalDateTime] with
            |    def mapping(v: com.google.protobuf.struct.Value, label: besom.types.Label): besom.util.Validated[besom.internal.DecodingError, java.time.LocalDateTime] =
            |      besom.internal.Decoder.stringDecoder.mapping(v, label).flatMap(str =>
            |        scala.util.Try(java.time.LocalDateTime.parse(v.getStringValue, java.time.format.DateTimeFormatter.ISO_DATE_TIME)) match
            |          case scala.util.Success(value) =>
            |            besom.util.Validated.valid(value)
            |          case scala.util.Failure(_) =>
            |            besom.util.Validated.invalid(besom.internal.Decoder.error(s"$label: Expected a LocalDateTime, got: '${v.kind}'", label))
            |      )
            |
            |
            |  given besom.Encoder[java.time.LocalDate] with
            |    def encode(t: java.time.LocalDate)(using besom.Context): besom.internal.Result[(besom.internal.Metadata, com.google.protobuf.struct.Value)] =
            |      besom.internal.Encoder.stringEncoder.encode(t.format(java.time.format.DateTimeFormatter.ISO_DATE))
            |
            |
            |  given besom.Decoder[java.time.LocalDate] with
            |    def mapping(v: com.google.protobuf.struct.Value, label: besom.types.Label): besom.util.Validated[besom.internal.DecodingError, java.time.LocalDate] =
            |      besom.internal.Decoder.stringDecoder.mapping(v, label).flatMap(str =>
            |        scala.util.Try(java.time.LocalDate.parse(v.getStringValue, java.time.format.DateTimeFormatter.ISO_DATE)) match
            |          case scala.util.Success(value) =>
            |            besom.util.Validated.valid(value)
            |          case scala.util.Failure(_) =>
            |            besom.util.Validated.invalid(besom.internal.Decoder.error(s"$label: Expected a LocalDate, got: '${v.kind}'", label))
            |     )
            |
            |  extension [A](output: besom.types.Output[scala.Option[A]])
            |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
            |      output.flatMap(
            |        _.map(f)
            |          .getOrElse(output.map(_ => scala.None))
            |      )
            |
            |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
            |      flatMapOpt(f(_).map(Some(_)))
            |""".stripMargin
      )
    ),
    Data(
      name = "Nested object",
      yaml = """apiVersion: apiextensions.k8s.io/v1
          |kind: CustomResourceDefinition
          |spec:
          |  group: example.com
          |  versions:
          |    - name: v1
          |      schema:
          |        openAPIV3Schema:
          |          type: object
          |          properties:
          |            spec:
          |              type: object
          |              properties:
          |                foo:
          |                  type: object
          |                  properties:
          |                    bar:
          |                      type: object
          |                      properties:
          |                        str:
          |                          type: string
          |  names:
          |    singular: nestedObject
          |    kind: NestedObject
          |""".stripMargin,
      expected = Map(
        "nestedObject/v1/Foo.scala" ->
          """package nestedObject.v1
            |
            |final case class Foo private(
            |  bar: besom.types.Output[scala.Option[nestedObject.v1.foo.Bar]],
            |) derives besom.Decoder, besom.Encoder
            |
            |object Foo:
            |  def apply(
            |    bar: besom.types.Input.Optional[nestedObject.v1.foo.Bar] = scala.None
            |  )(using besom.types.Context): Foo =
            |    new Foo(
            |      bar = bar.asOptionOutput(isSecret = false)
            |    )
            |
            |  extension (cls: Foo) def withArgs(
            |    bar: besom.types.Input.Optional[nestedObject.v1.foo.Bar] = cls.bar
            |  )(using besom.types.Context): Foo =
            |    new Foo(
            |      bar = bar.asOptionOutput(isSecret = false)
            |    )
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[Foo])
            |      def bar: besom.types.Output[scala.Option[nestedObject.v1.foo.Bar]] = output.flatMap(_.bar)
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[Foo]])
            |      def bar: besom.types.Output[scala.Option[nestedObject.v1.foo.Bar]] = output.flatMapOpt(_.bar)
            |
            |
            |  extension [A](output: besom.types.Output[scala.Option[A]])
            |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
            |      output.flatMap(
            |        _.map(f)
            |          .getOrElse(output.map(_ => scala.None))
            |      )
            |
            |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
            |      flatMapOpt(f(_).map(Some(_)))
            |""".stripMargin,
        "nestedObject/v1/NestedObject.scala" ->
          """package nestedObject.v1
          |
          |final case class NestedObject private(
          |  foo: besom.types.Output[scala.Option[nestedObject.v1.Foo]],
          |) derives besom.Decoder, besom.Encoder
          |
          |object NestedObject:
          |  def apply(
          |    foo: besom.types.Input.Optional[nestedObject.v1.Foo] = scala.None
          |  )(using besom.types.Context): NestedObject =
          |    new NestedObject(
          |      foo = foo.asOptionOutput(isSecret = false)
          |    )
          |
          |  extension (cls: NestedObject) def withArgs(
          |    foo: besom.types.Input.Optional[nestedObject.v1.Foo] = cls.foo
          |  )(using besom.types.Context): NestedObject =
          |    new NestedObject(
          |      foo = foo.asOptionOutput(isSecret = false)
          |    )
          |
          |  given outputOps: {} with
          |    extension(output: besom.types.Output[NestedObject])
          |      def foo: besom.types.Output[scala.Option[nestedObject.v1.Foo]] = output.flatMap(_.foo)
          |
          |  given optionOutputOps: {} with
          |    extension(output: besom.types.Output[scala.Option[NestedObject]])
          |      def foo: besom.types.Output[scala.Option[nestedObject.v1.Foo]] = output.flatMapOpt(_.foo)
          |
          |
          |  extension [A](output: besom.types.Output[scala.Option[A]])
          |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
          |      output.flatMap(
          |        _.map(f)
          |          .getOrElse(output.map(_ => scala.None))
          |      )
          |
          |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
          |      flatMapOpt(f(_).map(Some(_)))
          |""".stripMargin,
        "nestedObject/v1/foo/Bar.scala" ->
          """package nestedObject.v1.foo
            |
            |final case class Bar private(
            |  str: besom.types.Output[scala.Option[String]],
            |) derives besom.Decoder, besom.Encoder
            |
            |object Bar:
            |  def apply(
            |    str: besom.types.Input.Optional[String] = scala.None
            |  )(using besom.types.Context): Bar =
            |    new Bar(
            |      str = str.asOptionOutput(isSecret = false)
            |    )
            |
            |  extension (cls: Bar) def withArgs(
            |    str: besom.types.Input.Optional[String] = cls.str
            |  )(using besom.types.Context): Bar =
            |    new Bar(
            |      str = str.asOptionOutput(isSecret = false)
            |    )
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[Bar])
            |      def str: besom.types.Output[scala.Option[String]] = output.flatMap(_.str)
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[Bar]])
            |      def str: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.str)
            |
            |
            |  extension [A](output: besom.types.Output[scala.Option[A]])
            |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
            |      output.flatMap(
            |        _.map(f)
            |          .getOrElse(output.map(_ => scala.None))
            |      )
            |
            |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
            |      flatMapOpt(f(_).map(Some(_)))
            |""".stripMargin
      )
    ),
    Data(
      name = "Description and require field",
      yaml = """apiVersion: apiextensions.k8s.io/v1
               |kind: CustomResourceDefinition
               |spec:
               |  group: example.com
               |  versions:
               |    - name: v1
               |      schema:
               |        openAPIV3Schema:
               |          type: object
               |          properties:
               |            spec:
               |              type: object
               |              required:
               |                - requiredField
               |              properties:
               |                requiredField:
               |                  type: string
               |                descField:
               |                  description: |-
               |                    Description line 1
               |                    Description line 2
               |                  type: string
               |  names:
               |    singular: descWithRequiredField
               |    kind: DescWithRequiredField
               |""".stripMargin,
      expected = Map(
        "descWithRequiredField/v1/DescWithRequiredField.scala" ->
          """package descWithRequiredField.v1
            |
            |final case class DescWithRequiredField private(
            |  requiredField: besom.types.Output[String],
            |  /**
            |   * Description line 1
            |   * Description line 2
            |   */
            |  descField: besom.types.Output[scala.Option[String]],
            |) derives besom.Decoder, besom.Encoder
            |
            |object DescWithRequiredField:
            |  def apply(
            |    requiredField: besom.types.Input[String],
            |    descField: besom.types.Input.Optional[String] = scala.None
            |  )(using besom.types.Context): DescWithRequiredField =
            |    new DescWithRequiredField(
            |      requiredField = requiredField.asOutput(isSecret = false),
            |      descField = descField.asOptionOutput(isSecret = false)
            |    )
            |
            |  extension (cls: DescWithRequiredField) def withArgs(
            |    requiredField: besom.types.Input[String] = cls.requiredField,
            |    descField: besom.types.Input.Optional[String] = cls.descField
            |  )(using besom.types.Context): DescWithRequiredField =
            |    new DescWithRequiredField(
            |      requiredField = requiredField.asOutput(isSecret = false),
            |      descField = descField.asOptionOutput(isSecret = false)
            |    )
            |
            |  given outputOps: {} with
            |    extension(output: besom.types.Output[DescWithRequiredField])
            |      def requiredField: besom.types.Output[String] = output.flatMap(_.requiredField)
            |      def descField: besom.types.Output[scala.Option[String]] = output.flatMap(_.descField)
            |
            |  given optionOutputOps: {} with
            |    extension(output: besom.types.Output[scala.Option[DescWithRequiredField]])
            |      def requiredField: besom.types.Output[scala.Option[String]] = output.mapOpt(_.requiredField)
            |      def descField: besom.types.Output[scala.Option[String]] = output.flatMapOpt(_.descField)
            |
            |
            |  extension [A](output: besom.types.Output[scala.Option[A]])
            |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
            |      output.flatMap(
            |        _.map(f)
            |          .getOrElse(output.map(_ => scala.None))
            |      )
            |
            |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
            |      flatMapOpt(f(_).map(Some(_)))
            |""".stripMargin
      )
    ),
    Data(
      name = "Enum file",
      yaml = """apiVersion: apiextensions.k8s.io/v1
               |kind: CustomResourceDefinition
               |spec:
               |  group: example.com
               |  versions:
               |    - name: v1
               |      schema:
               |        openAPIV3Schema:
               |          type: object
               |          properties:
               |            spec:
               |              type: object
               |              properties:
               |                enumFile:
               |                  type: string
               |                  enum:
               |                    - enum1
               |                    - enum2
               |  names:
               |    singular: enumTest
               |    kind: EnumTest
               |""".stripMargin,
      expected = Map(
        "enumTest/v1/EnumFile.scala" ->
          """package enumTest.v1
            |
            |enum EnumFile:
            |  case enum1 extends EnumFile
            |  case enum2 extends EnumFile
            |
            |object EnumFile:
            |
            |  given besom.Encoder[EnumFile] with
            |    def encode(e: EnumFile)(using besom.Context): besom.internal.Result[(besom.internal.Metadata, com.google.protobuf.struct.Value)] =
            |      besom.internal.Encoder.stringEncoder.encode(e.toString)
            |
            |
            |  given besom.Decoder[EnumFile] with
            |    def mapping(v: com.google.protobuf.struct.Value, label: besom.types.Label): besom.util.Validated[besom.internal.DecodingError, EnumFile] =
            |      besom.internal.Decoder.stringDecoder.mapping(v, label).flatMap(str =>
            |        scala.util.Try(EnumFile.valueOf(str)) match
            |          case scala.util.Success(value) =>
            |            besom.util.Validated.valid(value)
            |          case scala.util.Failure(_) =>
            |            besom.util.Validated.invalid(besom.internal.Decoder.error(s"$label: Expected a EnumFile enum, got: '${v.kind}'", label))
            |      )
            |""".stripMargin,
        "enumTest/v1/EnumTest.scala" ->
          """package enumTest.v1
          |
          |final case class EnumTest private(
          |  enumFile: besom.types.Output[scala.Option[enumTest.v1.EnumFile]],
          |) derives besom.Decoder, besom.Encoder
          |
          |object EnumTest:
          |  def apply(
          |    enumFile: besom.types.Input.Optional[enumTest.v1.EnumFile] = scala.None
          |  )(using besom.types.Context): EnumTest =
          |    new EnumTest(
          |      enumFile = enumFile.asOptionOutput(isSecret = false)
          |    )
          |
          |  extension (cls: EnumTest) def withArgs(
          |    enumFile: besom.types.Input.Optional[enumTest.v1.EnumFile] = cls.enumFile
          |  )(using besom.types.Context): EnumTest =
          |    new EnumTest(
          |      enumFile = enumFile.asOptionOutput(isSecret = false)
          |    )
          |
          |  given outputOps: {} with
          |    extension(output: besom.types.Output[EnumTest])
          |      def enumFile: besom.types.Output[scala.Option[enumTest.v1.EnumFile]] = output.flatMap(_.enumFile)
          |
          |  given optionOutputOps: {} with
          |    extension(output: besom.types.Output[scala.Option[EnumTest]])
          |      def enumFile: besom.types.Output[scala.Option[enumTest.v1.EnumFile]] = output.flatMapOpt(_.enumFile)
          |
          |
          |  extension [A](output: besom.types.Output[scala.Option[A]])
          |    def flatMapOpt[B](f: A => besom.types.Output[Option[B]]): besom.types.Output[scala.Option[B]] =
          |      output.flatMap(
          |        _.map(f)
          |          .getOrElse(output.map(_ => scala.None))
          |      )
          |
          |    def mapOpt[B](f: A => besom.types.Output[B]): besom.types.Output[scala.Option[B]] =
          |      flatMapOpt(f(_).map(Some(_)))
          |""".stripMargin
      )
    )
  ).foreach(data =>
    test(data.name.withTags(data.tags)) {
      if (data.expectedError.isDefined)
        interceptMessage[Exception](data.expectedError.get)(ClassGenerator.createSourceFiles(data.yaml))
      else
        ClassGenerator.createSourceFiles(data.yaml) match
          case Left(ex) =>
            fail(s"Error: $ex")
          case Right(sourceFiles) =>
            sourceFiles.foreach {
              case SourceFile(FilePath(f: String), code: String) if data.expected.contains(f) =>
                assertNoDiff(code, data.expected(f))
                code.parse[Source].get
              case SourceFile(FilePath(f: String), _: String) if data.ignored.contains(f) =>
                println(s"Ignoring file: $f")
              case SourceFile(filename, _) =>
                fail(s"Unexpected file: ${filename.osSubPath}")
            }
    }
  )
}
