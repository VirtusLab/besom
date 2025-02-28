package com.virtuslab.child_lambda_b

import yaga.extensions.aws.lambda.LambdaHandler

case class Foo(str: String)
case class Bar(foo: Foo)
case class Baz(str: String)

class ChildLambdaB extends LambdaHandler[Unit, Bar, Baz]:
  override def handleInput(event: Bar) =
    println(s"Received input: $event")

    Baz(event.foo.str.reverse)
