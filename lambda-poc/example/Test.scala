package example

import spray.json._, DefaultJsonProtocol._

object Helpers:
  def helperMethod = transitiveHelperMethod + TransitiveHelpers.helperMethod

  def transitiveHelperMethod = 123

  def unusedHelperMethod = 234

object TransitiveHelpers:
  def helperMethod = transitiveHelperMethod

  def transitiveHelperMethod = 12

  def unusedHelperMethod = 23

val topLevelVal = "topLevel"

case class Request(value: String):
  val myFoo = new auxdefs.Foo(123)

object Request:
  given JsonFormat[Request] = jsonFormat1(Request.apply)

val lambda1 = lambdamacro.Lambda[String, String]("lambda1") { str =>
  val req = str.parseJson.fromJson[Request]
  val tlv = topLevelVal
  val help = Helpers.helperMethod
  println(req.value)
  println(topLevelVal)
  println(Helpers.helperMethod)
  import auxdefs.Bar
  println(new Bar(topLevelVal))
  req.value
}

val lambda2 = lambdamacro.Lambda[String, Request]("lambda2") { str =>
  Request("foo bar")
}