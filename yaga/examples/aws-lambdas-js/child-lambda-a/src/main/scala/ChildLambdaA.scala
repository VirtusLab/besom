package com.virtuslab.child_lambda_a

import yaga.extensions.aws.lambda.LambdaHandler

case class Foo(str: String)
case class Bar(foo: Foo)

class ChildLambdaA extends LambdaHandler[Unit, Bar, Unit]:
  override def handleInput(event: Bar) =
    println(s"Received input: $event")
    val upperCased = event.foo.str.toUpperCase
    println(upperCased)
