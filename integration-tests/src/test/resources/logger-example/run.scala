//> using scala "3.3.1"
//> using plugin "org.virtuslab::besom-compiler-plugin:0.1.0"
//> using dep "org.virtuslab::besom-core:0.1.0"
//> using options -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement

@main def run = Pulumi.run {
  for
    _ <- p"Hello, world!"
    _ <- log.warn("Nothing to do.")
  yield Pulumi.exports()
}