package yaga.generated.lambdatest.child // package name derived from original code's package

import besom.json.defaultProtocol
import besom.json.defaultProtocol.given

case class Foo(
  str: scala.Predef.String
) derives besom.json.JsonFormat

case class Bar(
  foo: Foo
) derives besom.json.JsonFormat

case class Baz(
  str: scala.Predef.String
) derives besom.json.JsonFormat
