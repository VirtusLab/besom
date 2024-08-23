package besom.experimental

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

  Stack(sourceStack.name)
}
