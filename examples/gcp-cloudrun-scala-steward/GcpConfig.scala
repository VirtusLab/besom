import besom.*

case class GcpConfig(
  project: Output[String],
  region: Output[String]
)
object GcpConfig:
  extension (o: Output[GcpConfig])
    def project: Output[String] = o.flatMap(_.project)
    def region: Output[String]  = o.flatMap(_.region)

  def apply(using Context): GcpConfig =
    GcpConfig(
      project = config.requireString("gcp:project"),
      region = config.requireString("gcp:region")
    )
