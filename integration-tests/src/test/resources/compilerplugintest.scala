//> using scala 3.3.1
//> using plugin "org.virtuslab::besom-compiler-plugin:0.1.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-core:0.1.1-SNAPSHOT"

import besom.*

case class Data(name: Output[String], age: Int)

@main
def main = Pulumi.run {
  val name: Output[String] = Output("Biden")

  val stringOut = Output(
    s"Joe ${name}" // error
  )

  val anUnusedOutput = Output("unused") // error

  val anOutput = Output("XD")
  val anotherOutput = anOutput match {
    case out => Output("XD") // error
  }

  def doStuffThatAintRight(): Unit = {
    val anOutput = Output("XD")
    anOutput // error
  }

  def getAnotherOutput() =
    Output("XD").flatMap { _ =>
      Output("XD")
    }

  def getAnOutput() = Output("XD")

  getAnOutput() // error

  anOutput // error

  val yetAnotherOutput = { // error
    getAnOutput() // error
    Output("XD")
  }

  val anOutputOfAnOutput = Output(anOutput)

  val outputList = List(Output("First"), Output("Second"), anOutput)
  // should we special case foreach?
  outputList.foreach { output => output } // should be an error, but foreach has the signature of ~`[U] => List[A] => (A => U) => Unit`
  outputList.map { case IgnoredOutput(_) => } // correct, no error

  def takesAnOutputAndIgnoresIt(value: Output[String]): Unit = { // error
    // nothing done with value
  }

  takesAnOutputAndIgnoresIt(anOutput)

  def takesMultipleParameterLists(value: Output[String])(implicit age: Output[Int]): Unit = { // error
    value // error
  }

  implicit val ageOutput: Output[Int] = Output(30)
  takesMultipleParameterLists(anOutput) // Should detect that ageOutput is unused.

  val tupleWithOutput = (Output("First"), "Second")
  tupleWithOutput._1 // error

  val optionWithOutput: Option[Output[String]] = Some(Output("Hello"))
  optionWithOutput.map { case IgnoredOutput(_) => "Ignored" } // correct, no error
  optionWithOutput.map { output => "This should raise an error" } // error

  // case class Data(name: Output[String], age: Int)
  val myData = Data(Output("John"), 25)
  myData.name // error

  def takesVarargs(outputs: Output[String]*): Unit =
    outputs.foreach { _ => () } // error
  takesVarargs(anOutput, anotherOutput)

  for { // error
    x <- Output("X")
    y <- Output("Y")
    z <- for {
      a <- Output("A")
      IgnoredOutput(_) <- Output("B")
    } yield a
    w <- Output("W")
  } yield x

  for
    _ <- stringOut
    _ <- anotherOutput
    _ <- anOutputOfAnOutput // error
    IgnoredOutput(_) <- anOutputOfAnOutput
  yield
    Pulumi.exports(
      stringOut = stringOut
    )
}
