import besom.*
import besom.json.*

case class GitConfig(
  forgeLogin: String,
  forgeApiHost: String,
  forgeType: String,
  gitAuthorEmail: String,
  password: String
) derives JsonFormat
object GitConfig:
  extension (o: Output[GitConfig])
    def forgeLogin: Output[String]     = o.map(_.forgeLogin)
    def forgeApiHost: Output[String]   = o.map(_.forgeApiHost)
    def forgeType: Output[String]      = o.map(_.forgeType)
    def gitAuthorEmail: Output[String] = o.map(_.gitAuthorEmail)
    def password: Output[String]       = o.map(_.password)
