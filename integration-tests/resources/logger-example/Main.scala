import besom.*

@main def run = Pulumi.run {
  Stack(
    log.warn("Nothing here yet. It's waiting for you!"),
    p"Interpolated ${Output("value")}".flatMap(log.info(_))
  )
}
