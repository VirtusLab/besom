import besom.*
import besom.json.*

case class KeycloakConfig(
  user: String,
  password: String,
  host: String
) derives JsonFormat
object KeycloakConfig:
  extension (o: Output[KeycloakConfig])
    def user: Output[String]     = o.map(_.user)
    def password: Output[String] = o.map(_.password)
    def host: Output[String]     = o.map(_.host)
