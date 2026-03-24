package besom.internal

import besom.*
import besom.json.*

// mirror case classes with unwrapped fields for typed StackReference
case class DatabaseOutputs(host: String, port: Int, database: String, username: String, password: String) derives JsonReader, Encoder
case class InfraOutputs(vpcId: String, zone: String, port: Int, secretToken: String, db: DatabaseOutputs) derives JsonReader

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
      // nested struct with secret fields — exercises secret envelope unwrapping
      assert(outputs.db.host == "db-host.svc.local", s"db.host should be db-host.svc.local, got ${outputs.db.host}")
      assert(outputs.db.port == 5432, s"db.port should be 5432, got ${outputs.db.port}")
      assert(outputs.db.database == "mydb", s"db.database should be mydb, got ${outputs.db.database}")
      assert(outputs.db.username == "admin", s"db.username should be admin, got ${outputs.db.username}")
      assert(outputs.db.password == "hunter2", s"db.password should be hunter2, got ${outputs.db.password}")
    }
  }

  val secretCheck = typedSourceStack.flatMap { sourceStack =>
    Output {
      for s <- sourceStack.secretOutputNames.getData
      yield
        val names = s.getValueOrElse(Set.empty)
        assert(names.contains("secretToken"), s"secretToken should be in secret output names, got $names")
        assert(names.contains("db"), s"db should be in secret output names (has secret sub-fields), got $names")
    }
  }

  Stack(sanityCheck, secretCheck).exports(
    vpcId = typedSourceStack.map(_.outputs.vpcId),
    zone = typedSourceStack.map(_.outputs.zone),
    port = typedSourceStack.map(_.outputs.port),
    secretToken = typedSourceStack.map(_.outputs.secretToken),
    db = typedSourceStack.map(_.outputs.db)
  )
}
