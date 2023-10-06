import besom.*

@main def run = Pulumi.run {
  for
    _ <- log.warn("Nothing here yet. It's waiting for you!")
  yield exports()
}