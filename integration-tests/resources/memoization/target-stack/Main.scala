package besom.internal

import besom.*
import besom.json.*

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  val sourceStackName = config.requireString("sourceStack").map(NonEmptyString(_).get)
  // StackReference is a resource too so we want to verify that calls to it are idempotent
  def sourceStack = besom.StackReference(
    "stackRef",
    StackReferenceArgs(sourceStackName),
    StackReferenceResourceOptions()
  )

  val justForTesting = for 
    _ <- sourceStack
    _ <- sourceStack
  yield ()

  val sshKeyUrn = sourceStack.flatMap(
    _.getOutput("sshKeyUrn")
      .map {
        case Some(JsString(s)) => s
        case other             => throw RuntimeException(s"Expected string, got $other")
      }
      .flatMap(URN.parse)
  )

  val rsaBits = sourceStack.flatMap(_.getOutput("sshKeyRsaBits").map {
    case Some(JsNumber(d)) => d.toInt
    case other             => throw RuntimeException(s"Expected string, got $other")
  })

  val algo = sourceStack.flatMap(_.getOutput("sshKeyAlgo").map {
    case Some(JsString(s)) => s
    case other             => throw RuntimeException(s"Expected string, got $other")
  })

  Stack(justForTesting).exports(
    sshKeyUrn = sshKeyUrn,
    sshKeyRsaBits = rsaBits,
    sshKeyAlgo = algo
  )
}
