package besom.scripts

import scala.sys.exit
import scala.util.matching.Regex

import besom.model.SemanticVersion

object Version:
  def main(args: String*): Unit =
    val cwd               = besomDir
    lazy val besomVersion = os.read(cwd / "version.txt").trim

    lazy val besomDependencyPattern: Regex =
      ("""^(//> using (?:test\.)?(?:dep|lib|plugin) +"?\S+::besom-)([^"]+)("?)$""").r

    val expectedFileNames = Vector("project.scala", "project-test.scala", "run.scala")

    def projectFiles(path: os.Path = cwd): Map[os.Path, String] =
      println(s"Searching for project files in $path")
      os.walk(
        path,
        skip = (p: os.Path) => p.last == ".git" || p.last == ".out" || p.last == ".bsp" || p.last == ".scala-build" || p.last == ".idea"
      ).filter(f => expectedFileNames.contains(f.last))
        .map((f: os.Path) => f -> os.read(f))
        .toMap

    def changeVersion(version: String, newBesomVersion: String, packageVersion: String => Option[String] = _ => None): String =
      lazy val coreShortVersion = SemanticVersion
        .parseTolerant(besomVersion)
        .fold(
          e => throw Exception(s"Invalid besom version: ${besomVersion}", e),
          _.copy(patch = 0).toShortString
        )
      version match
        case s"$a:$b-core.$_" => s"$a:${packageVersion(a).getOrElse(b)}-core.$coreShortVersion"
        case s"$a:$_"         => s"$a:$newBesomVersion"
    end changeVersion

    args match
      case "show" :: Nil =>
        println(s"Showing all Besom dependencies")
        projectFiles()
          .foreachEntry { case (f, content) =>
            content.linesIterator.zipWithIndex.foreach { case (line, index) =>
              line match
                case besomDependencyPattern(prefix, version, suffix) =>
                  val changedLine = prefix + changeVersion(version, besomVersion) + suffix
                  println(s"$f:$index:\n$changedLine\n")
                case _ => // ignore
            }
          }
      case "summary" :: maybePath =>
        val path = maybePath.headOption.map(cwd / os.RelPath(_)).getOrElse(cwd)
        println(s"Showing all dependencies in $path")
        projectFiles(path)
          .flatMap { case (_, content) =>
            content.linesIterator.flatMap {
              case besomDependencyPattern(_, version, _) =>
                version match
                  case s"$a:$b-core.$c" => Seq(a -> b, "core" -> c)
                  case s"$a:$b"         => Seq(a -> b)
              case _ => Seq.empty
            }
          }
          .toSet
          .foreach { case (a, b) =>
            println(s"$a $b")
          }
      case "bump" :: newBesomVersion :: Nil =>
        println(s"Bumping Besom core version from '$besomVersion' to '$newBesomVersion'")
        os.write.over(cwd / "version.txt", newBesomVersion)
        println(s"Updated version.txt")
        projectFiles()
          .collect { case (path, content) if content.linesIterator.exists(besomDependencyPattern.matches) => path -> content }
          .foreachEntry { case (path, content) =>
            val newContent = content.linesIterator
              .map {
                case line if line.contains("besom-fake-") => line // ignore
                case besomDependencyPattern(prefix, version, suffix) =>
                  prefix + changeVersion(version, besomVersion) + suffix
                case line => line // pass through
              }
              .mkString("\n") + "\n"
            os.write.over(path, newContent)
            println(s"Updated $path")
          }
      case "update" :: Nil =>
        println(s"Bumping Besom packages version to latest")
        val latestPackages = latestPackageVersions()
        projectFiles()
          .collect {
            case (path, content) if content.linesIterator.exists(besomDependencyPattern.matches) => path -> content
          }
          .foreachEntry { case (path, content) =>
            val newContent = content.linesIterator
              .map {
                case line if line.contains("besom-fake-") => line // ignore
                case besomDependencyPattern(prefix, version, suffix) =>
                  prefix + changeVersion(version, besomVersion, latestPackages.get) + suffix
                case line => line // pass through
              }
              .mkString("\n") + "\n"
            os.write.over(path, newContent)
            println(s"Updated $path")
          }
      case cmd =>
        println(s"Unknown command: $cmd\n")
        println(
          s"""Usage: version <command>
             |
             |Commands:
             |  show               - show all project.scala files with besom dependency
             |  bump <new_version> - bump besom version in all project.scala files
             |  update             - update all provider dependencies in project.scala files to latest
             |  summary <path>     - summarize all dependencies in <path> (default: .)
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
