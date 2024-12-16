package lambdatest.child

import yaga.extensions.aws.lambda.LambdaHandler
import besom.json.* // TODO Simplify? We need only defaultProtocol and formats for primitive types

case class Foo(str: String) derives JsonFormat
case class Bar(foo: Foo) derives JsonFormat
case class Baz(str: String) derives JsonFormat

class ChildLambda extends LambdaHandler[Unit, Bar, Baz]:
  override def handleInput(event: Bar) =
    println(s"Received input: $event")

    Baz(event.foo.str.reverse)
