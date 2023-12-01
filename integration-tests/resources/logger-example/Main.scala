import besom.*

@main def run = Pulumi.run {
  for
    _ <- log.warn("Nothing here yet. It's waiting for you!")
    _ <- p"Interpolated ${Output("value")}".flatMap(log.info(_))
  yield exports()
}