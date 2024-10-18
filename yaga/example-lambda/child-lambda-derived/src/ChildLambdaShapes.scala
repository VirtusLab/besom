package yaga.generated.lambdatest.child // package name derived from original code's package

import besom.json.* // TODO Simplify? We need only defaultProtocol and formats for primitive types

case class Foo(str: String) derives besom.json.JsonFormat
case class Bar(foo: Foo) derives besom.json.JsonFormat
case class Baz(str: String) derives besom.json.JsonFormat
