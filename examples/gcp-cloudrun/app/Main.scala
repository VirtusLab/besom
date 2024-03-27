package org.virtuslab.besom.example

import sttp.tapir.*
import sttp.tapir.server.jdkhttp.*
import java.util.concurrent.Executors

@main def main(): Unit =
  // required by Cloud Run Container runtime contract https://cloud.google.com/run/docs/reference/container-contract
  val host = "0.0.0.0"
  val port = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8080)
  val handler = endpoint.get
    .out(stringBody)
    .handle(_ => Right(s"Hello, World!"))

  println(s"Starting server on $host:$port")
  val _ = JdkHttpServer()
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .host(host)
    .port(port)
    .addEndpoint(handler)
    .start()
