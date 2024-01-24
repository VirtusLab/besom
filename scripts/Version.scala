package besom.scripts

import scala.sys.exit
import scala.util.CommandLineParser.FromString
import scala.util.matching.Regex

object Version:
  def main(args: String*): Unit =
    val cwd               = besomDir
    lazy val besomVersion = os.read(cwd / "version.txt").trim

    lazy val besomDependencyPattern: Regex =
      ("""^(//> using (?:test\.)?(?:dep|lib|plugin) +"?\S+::besom-)([^"]+)("?)$""").r

    val expectedFileNames = Vector("project.scala", "project-test.scala", "run.scala")

    lazy val projectFiles: Map[os.Path, String] =
      println(s"Searching for project files in $cwd")
      os.walk(cwd)
        .filter(f => expectedFileNames.contains(f.last))
        .filterNot(_.startsWith(cwd / ".out"))
        .map((f: os.Path) => f -> os.read(f))
        .toMap

    def changeVersion(version: String, newBesomVersion: String, packageVersion: String => Option[String] = _ => None): String =
      version match
        case s"$a:$b-core.$_" => s"$a:${packageVersion(a).getOrElse(b)}-core.$newBesomVersion"
        case s"$a:$_"         => s"$a:$newBesomVersion"
    end changeVersion

    args match
      case "show" :: Nil =>
        println(s"Showing all Besom dependencies")
        projectFiles
          .foreachEntry { case (f, content) =>
            content.linesIterator.zipWithIndex.foreach { case (line, index) =>
              line match
                case besomDependencyPattern(prefix, version, suffix) =>
                  val changedLine = prefix + changeVersion(version, besomVersion) + suffix
                  println(s"$f:$index:\n$changedLine\n")
                case _ => // ignore
            }
          }
      case "bump" :: newBesomVersion :: Nil =>
        println(s"Bumping Besom core version from '$besomVersion' to '$newBesomVersion'")
        projectFiles
          .collect { case (path, content) if content.linesIterator.exists(besomDependencyPattern.matches) => path -> content }
          .foreachEntry { case (path, content) =>
            val newContent = content.linesIterator
              .map {
                case besomDependencyPattern(prefix, version, suffix) =>
                  prefix + changeVersion(version, besomVersion) + suffix
                case line => line // pass through
              }
              .mkString("\n") + "\n"
            os.write.over(path, newContent)
            println(s"Updated $path")
          }

        os.write.over(cwd / "version.txt", newBesomVersion)
        println(s"Updated version.txt")
      case "update" :: Nil =>
        println(s"Bumping Besom packages version to latest")
        val latestPackages = latestPackageVersions()
        projectFiles
          .collect { case (path, content) if content.linesIterator.exists(besomDependencyPattern.matches) => path -> content }
          .foreachEntry { case (path, content) =>
            val newContent = content.linesIterator
              .map {
                case besomDependencyPattern(prefix, version, suffix) =>
                  prefix + changeVersion(version, besomVersion, latestPackages.get) + suffix
                case line => line // pass through
              }
              .mkString("\n") + "\n"
            os.write.over(path, newContent)
            println(s"Updated $path")
          }
      case _ =>
        println(
          s"""Usage: version <command>
           |
           |Commands:
           |  show               - show all project.scala files with besom dependency
           |  bump <new_version> - bump besom version in all project.scala files
           |  update             - update all provider dependencies in project.scala files to latest
           |""".stripMargin
        )
        exit(1)
    end match
  end main

  private def latestPackageVersions(): Map[String, String] =
    println(s"Searching for latest package versions")
    Packages
      .readPackagesMetadata(Packages.packagesDir)
      .map { metadata =>
        metadata.name -> metadata.version.getOrElse(throw Exception("Package version must be present after generating")).asString
      }
      .toMap
  end latestPackageVersions
end Version
