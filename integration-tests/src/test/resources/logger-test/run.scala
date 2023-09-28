//> using scala 3.3.1
//> using plugin "org.virtuslab::besom-compiler-plugin:0.0.2-SNAPSHOT"
//> using lib "org.virtuslab::besom-core:0.0.2-SNAPSHOT"

import besom.*

@main
def main = Pulumi.run {
  log.info("Nothing here yet. It's waiting for you!")
  for _ <- log.info("Nothing here yet. It's waiting for you!")
  yield Pulumi.exports()
}
