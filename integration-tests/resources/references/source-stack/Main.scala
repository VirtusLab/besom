import besom.*
import besom.api.tls

case class DummyStructuredOutput(
  a: Output[String],
  b: Double
) derives Encoder

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  val sshKey = tls.PrivateKey(
    "sshKey",
    tls.PrivateKeyArgs(
      algorithm = "RSA",
      rsaBits = 4096
    )
  )

  Output {
    exports(
      sshKeyUrn = sshKey.urn,
      value1 = 23,
      value2 = "Hello world!",
      structured = DummyStructuredOutput(Output.secret("ABCDEF"), 42.0)
    )
  }
}
