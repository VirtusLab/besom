package besom.scripts

@main def main(args: String*): Unit =
  val _ = besomDir

  args.headOption.getOrElse("") match
    case "schemas"  => Schemas.main(args.tail*)
    case "packages" => Packages.main(args.tail*)
    case "proto"    => Proto.main(args.tail*)
    case "coverage" => Coverage.main(args.tail*)
    case "version"  => Version.main(args.tail*)
    case cmd =>
      println(s"Unknown command: $cmd\n")
      println(
        s"""Usage: cli <command>
           |  schemas  - fetch upstream test schemas
           |  packages - generate and publish Besom packages
           |  proto    - fetch and compile Pulumi gRPC proto files
           |  coverage - generate test coverage report
           |  version  - bump and update version in project.scala files
           |""".stripMargin
      )
