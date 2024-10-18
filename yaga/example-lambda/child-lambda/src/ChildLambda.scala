//> using scala 3.3.3

//> using dep org.virtuslab::yaga-aws:0.4.0-SNAPSHOT


package lambdatest.child

import yaga.extensions.aws.lambda.ShapedRequestHandler
import com.amazonaws.services.lambda.runtime.Context

import besom.json.* // TODO Simplify? We need only defaultProtocol and formats for primitive types

case class Foo(str: String) derives JsonFormat
case class Bar(foo: Foo) derives JsonFormat
case class Baz(str: String) derives JsonFormat

// TODO Schemas should be valid json
// TODO Annotations should be added by a compiler plugin based on the type arguments
// TODO Add version to annotations?
@yaga.extensions.aws.lambda.annotations.ConfigSchema("""Struct(ArraySeq())""")
@yaga.extensions.aws.lambda.annotations.InputSchema("""Struct(List((foo,Struct(List((str,String))))))""")
@yaga.extensions.aws.lambda.annotations.OutputSchema("""Struct(List((str,String)))""")
class ChildLambda extends ShapedRequestHandler[Unit, Bar, Baz]:
  override def handleInput(event: Bar, context: Context): Baz =
    Baz(s"Got nested input: ${event.foo.str}")
