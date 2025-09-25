package besom.scripts

import besom.codegen.Config

@main def main(args: String*): Unit =
  val _ = Config.besomDir

  args.headOption.getOrElse("") match
    case "schemas"  => Schemas.main(args.tail*)
    case "packages" => Packages.main(args.tail*)
    case "proto"    => Proto.main(args.tail*)
    case "coverage" => Coverage.main(args.tail*)
    case "version"  => Version.main(args.tail*)
    case "jars"     => JarVersions.main(args.tail.headOption.getOrElse(os.pwd.toString))
    case cmd =>
      println(s"Unknown command: $cmd\n")
      println(
        s"""Usage: cli <command>
           |  schemas  - fetch upstream test schemas
           |  packages - generate and publish Besom packages
           |  proto    - fetch and compile Pulumi gRPC proto files
           |  coverage - generate test coverage report
           |  version  - bump and update version in project.scala files
           |  jars     - check that all jars in directory conform to the binary versioning scheme
           |""".stripMargin
      )
