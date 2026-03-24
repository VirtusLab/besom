import besom.*

case class DatabaseConfig(
  host: Output[String],
  port: Output[Int],
  database: Output[String],
  username: Output[String],
  password: Output[String]
) derives Encoder

case class InfraOutputs(
  vpcId: Output[String],
  zone: Output[String],
  port: Output[Int],
  secretToken: Output[String],
  db: Output[DatabaseConfig]
) derives Encoder

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  Stack.exports(
    InfraOutputs(
      vpcId = Output("vpc-abc123"),
      zone = Output("us-east-1"),
      port = Output(8080),
      secretToken = Output.secret("super-secret-token"),
      db = Output(
        DatabaseConfig(
          host = Output("db-host.svc.local"),
          port = Output(5432),
          database = Output("mydb"),
          username = Output.secret("admin"),
          password = Output.secret("hunter2")
        )
      )
    )
  )
}
