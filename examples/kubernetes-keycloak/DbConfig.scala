import besom.*
import besom.json.*

case class DbConfig(
  user: String,
  password: String,
  database: String
) derives JsonFormat
object DbConfig:
  extension (o: Output[DbConfig])
    def user: Output[String]     = o.map(_.user)
    def password: Output[String] = o.map(_.password)
    def database: Output[String] = o.map(_.database)
