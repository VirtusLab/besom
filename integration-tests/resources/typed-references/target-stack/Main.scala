package besom.internal

import besom.*
import besom.json.*

// mirror case class with unwrapped fields for typed StackReference
case class InfraOutputs(vpcId: String, zone: String, port: Int, secretToken: String) derives JsonReader

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {

  val sourceStackName = config.requireString("sourceStack").map(NonEmptyString(_).get)
  val typedSourceStack = StackReference[InfraOutputs](
    "stackRef",
    StackReferenceArgs(sourceStackName),
    StackReferenceResourceOptions()
  )

  val sanityCheck = typedSourceStack.flatMap { sourceStack =>
    val outputs = sourceStack.outputs

    Output {
      assert(outputs.vpcId == "vpc-abc123", s"vpcId should be vpc-abc123, got ${outputs.vpcId}")
      assert(outputs.zone == "us-east-1", s"zone should be us-east-1, got ${outputs.zone}")
      assert(outputs.port == 8080, s"port should be 8080, got ${outputs.port}")
      assert(outputs.secretToken == "super-secret-token", s"secretToken should be super-secret-token, got ${outputs.secretToken}")
    }
  }

  val secretCheck = typedSourceStack.flatMap { sourceStack =>
    Output {
      for
        s <- sourceStack.secretOutputNames.getData
      yield
        val names = s.getValueOrElse(Set.empty)
        assert(names.contains("secretToken"), s"secretToken should be in secret output names, got $names")
    }
  }

  Stack(sanityCheck, secretCheck).exports(
    vpcId = typedSourceStack.map(_.outputs.vpcId),
    zone = typedSourceStack.map(_.outputs.zone),
    port = typedSourceStack.map(_.outputs.port),
    secretToken = typedSourceStack.map(_.outputs.secretToken)
  )
}
