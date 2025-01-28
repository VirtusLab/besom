package com.virtuslab.child_lambda_a

import yaga.extensions.aws.lambda.LambdaHandler
import besom.json.* // TODO Simplify? We need only defaultProtocol and formats for primitive types

case class Foo(str: String) derives JsonFormat
case class Bar(foo: Foo) derives JsonFormat

class ChildLambdaA extends LambdaHandler[Unit, Bar, Unit]:
  override def handleInput(event: Bar) =
    println(s"Received input: $event")
    val upperCased = event.foo.str.toUpperCase
    println(upperCased)
