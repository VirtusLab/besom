import besom.*

case class InfraOutputs(
  vpcId: Output[String],
  zone: Output[String],
  port: Output[Int],
  secretToken: Output[String]
) derives Encoder

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  Stack.exports(
    InfraOutputs(
      vpcId = Output("vpc-abc123"),
      zone = Output("us-east-1"),
      port = Output(8080),
      secretToken = Output.secret("super-secret-token")
    )
  )
}
