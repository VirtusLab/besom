package org.virtuslab.besom.example

import redis.clients.jedis.{JedisPooled, UnifiedJedis}
import sttp.tapir.*
import sttp.tapir.server.jdkhttp.*

import scala.util.Try

@main def main(): Unit =
  val port      = sys.env.get("APP_PORT").flatMap(_.toIntOption).getOrElse(8080)
  val redisPort = sys.env.get("REDIS_PORT").flatMap(_.toIntOption).getOrElse(6379)
  val redisHost = sys.env.getOrElse("REDIS_HOST", "localhost")

  val redisKey = "hits"
  val pool     = JedisPooled(redisHost, redisPort)

  def upsert(client: UnifiedJedis)(key: String): String =
    val num = client.get(key) match
      case null => 1
      case v    => v.toInt + 1

    client.set(key, s"$num")
    s"I have been viewed ${num} time(s)."
  end upsert

  val handler = endpoint.get
    .out(stringBody)
    .handle { * =>
      Try(upsert(pool)(redisKey)).toEither.left.map { e => println(s"Error: $e") }
    }

  println(s"Starting server on port $port, using redis at $redisHost:$redisPort")

  val _ = JdkHttpServer()
    .port(port)
    .addEndpoint(handler)
    .start()

end main
