package besom.internal

import besom.*
import besom.json.*

// mirror case classes for typed StackReference — secret fields use Output[A] to preserve per-field secrecy
case class DatabaseOutputs(
  host: Output[String],
  port: Output[Int],
  database: Output[String],
  username: Output[String],
  password: Output[String]
) derives JsonReader,
      Encoder

case class InfraOutputs(
  vpcId: String,
  zone: String,
  port: Int,
  secretToken: Output[String],
  db: DatabaseOutputs
) derives JsonReader

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

    for
      vpcId    <- Output(outputs.vpcId)
      zone     <- Output(outputs.zone)
      port     <- Output(outputs.port)
      token    <- outputs.secretToken
      host     <- outputs.db.host
      dbPort   <- outputs.db.port
      database <- outputs.db.database
      username <- outputs.db.username
      password <- outputs.db.password
    yield
      assert(vpcId == "vpc-abc123", s"vpcId should be vpc-abc123, got $vpcId")
      assert(zone == "us-east-1", s"zone should be us-east-1, got $zone")
      assert(port == 8080, s"port should be 8080, got $port")
      assert(token == "super-secret-token", s"secretToken should be super-secret-token, got $token")
      assert(host == "db-host.svc.local", s"db.host should be db-host.svc.local, got $host")
      assert(dbPort == 5432, s"db.port should be 5432, got $dbPort")
      assert(database == "mydb", s"db.database should be mydb, got $database")
      assert(username == "admin", s"db.username should be admin, got $username")
      assert(password == "hunter2", s"db.password should be hunter2, got $password")
  }

  val secretCheck = typedSourceStack.flatMap { sourceStack =>
    Output {
      for
        s <- sourceStack.secretOutputNames.getData
        // verify per-field secrecy on Output[String] fields
        tokenSecret    <- sourceStack.outputs.secretToken.getData.map(_.secret)
        usernameSecret <- sourceStack.outputs.db.username.getData.map(_.secret)
        passwordSecret <- sourceStack.outputs.db.password.getData.map(_.secret)
        hostSecret     <- sourceStack.outputs.db.host.getData.map(_.secret)
      yield
        val names = s.getValueOrElse(Set.empty)
        assert(names.contains("secretToken"), s"secretToken should be in secret output names, got $names")
        assert(names.contains("db"), s"db should be in secret output names (has secret sub-fields), got $names")
        // per-field secrecy checks
        assert(tokenSecret, "secretToken Output should be marked as secret")
        assert(usernameSecret, "db.username Output should be marked as secret")
        assert(passwordSecret, "db.password Output should be marked as secret")
        assert(!hostSecret, "db.host Output should NOT be marked as secret")
    }
  }

  Stack(sanityCheck, secretCheck).exports(
    vpcId = typedSourceStack.map(_.outputs.vpcId),
    zone = typedSourceStack.map(_.outputs.zone),
    port = typedSourceStack.map(_.outputs.port),
    secretToken = typedSourceStack.flatMap(_.outputs.secretToken),
    db = typedSourceStack.map(_.outputs.db)
  )
}
