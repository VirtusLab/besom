//> using scala 3.3.1

//> using dep com.lihaoyi::os-lib:0.9.2

import scala.sys.exit
import scala.util.CommandLineParser.FromString
import scala.util.matching.Regex

@main def main(command: String, other: String*): Unit =
  if os.pwd.last != "besom" then
    println("You have to run this command from besom project root directory!")
    sys.exit(1)

  val besomVersion = os.read(os.pwd / "version.txt").trim

  val besomDependencyPattern: Regex =
    ("""^(//> using (?:test\.)?dep ["]{0,1}org.virtuslab::besom-[a-z]+:(?:[0-9\.]+-core\.)?)""" + besomVersion + """(["]{0,1})$""").r

  val projectFiles: Map[os.Path, String] =
    os.walk(os.pwd)
      .filter(_.last == "project.scala")
      .filterNot(_.startsWith(os.pwd / ".out"))
      .map((f: os.Path) => f -> os.read(f))
      .toMap

  command match
    case "show" =>
      projectFiles
        .foreachEntry { case (f, content) =>
          content.linesIterator.zipWithIndex.foreach { case (line, index) =>
            if besomDependencyPattern.matches(line) then
              println(s"$f:$index:\n$line\n")
          }
        }
    case "bump" =>
      val newBesomVersion =
        if other.isDefinedAt(0) then other.toVector(0)
        else {
          println("You have to provide new besom version as the second argument.")
          sys.exit(1)
        }

      projectFiles
        .collect { case (path, content) if content.linesIterator.exists(besomDependencyPattern.matches) => path -> content }
        .foreachEntry { case (path, content) =>
          val newContent = content.linesIterator
            .map {
              case besomDependencyPattern(prefix, suffix) => prefix + newBesomVersion + suffix
              case line                                   => line
            }
            .mkString("\n") + "\n"

          os.write.over(path, newContent)
          println(s"Updated $path")
        }

      // TODO: automatically update provider versions based on Packages.scala

      os.write.over(os.pwd / "version.txt", newBesomVersion)
      println(s"Updated version.txt")
    case _ =>
      println(
        s"""Usage: version <command>
         |
         |Commands:
         |  show               - show all project.scala files with besom dependency
         |  bump <new_version> - bump besom version in all project.scala files
         |""".stripMargin
      )
      exit(1)
  end match
end main
