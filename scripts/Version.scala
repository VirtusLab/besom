package besom.scripts

import besom.codegen.Config
import besom.model.SemanticVersion

import scala.sys.exit
import scala.util.Try
import scala.util.matching.Regex

object Version:
  /** Matches scala-cli besom dependencies and plugins used for version rewriting.
    *
    * We have three capturing groups:
    *   - the dependency prefix for a library or plugin, also test scope is included
    *   - the name and version that will be processed outside of the regexp
    *   - the suffix, an optional double-quote
    *
    * We support and preserve both double-quoted and non double-quoted syntax.
    */
  private[scripts] lazy val besomDependencyPattern: Regex =
    """^(//> using (?:test\.)?(?:dep|lib|plugin) +"?\S+::besom-)([^"]+)("?)$""".r

  private val cwd = Config.besomDir

  def main(args: String*): Unit =
    given config: Config = Config()

    args match
      case "show" :: Nil =>
        println(s"Besom version: ${config.besomVersion}\n")
        println(s"Showing all Besom dependencies...\n")
        projectFiles()
          .foreachEntry { case (f, content) =>
            content.linesIterator.zipWithIndex.foreach { case (line, index) =>
              line match
                case besomDependencyPattern(prefix, version, suffix) =>
                  val changedLine = prefix + changeVersion(version, config.besomVersion) + suffix
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
      case "bump" :: newBesomVersionStr :: Nil =>
        val (newBesomVersion, isSnapshot) = SemanticVersion
          .parse(newBesomVersionStr)
          .fold(
            e => throw Exception(s"Invalid version: $newBesomVersionStr", e),
            v => (v.toString, v.isSnapshot)
          )
        println(s"Bumping Besom core version from '${config.besomVersion}' to '$newBesomVersion'")
        os.write.over(cwd / "version.txt", newBesomVersion)
        println(s"Updated version.txt")
        val filesWithBesomDeps = projectFiles()
          .collect { case (path, content) if content.linesIterator.exists(besomDependencyPattern.matches) => path -> content }
        filesWithBesomDeps
          .foreachEntry { case (path, content) =>
            val hasSnapshotRepo = content.linesIterator
              .forall(line => line.contains("repository snapshots"))
            val newContent: Vector[String] = content.linesIterator.toVector
              .map {
                case line if line.contains("besom-fake-") => line // ignore
                case besomDependencyPattern(prefix, version, suffix) =>
                  prefix + changeVersion(version, newBesomVersion) + suffix
                case line => line // pass through
              }
              .filter {
                case line if !isSnapshot && line.contains("repository snapshots") =>
                  false // remove snapshot repo from non-snapshot version
                case _ => true
              } ++ {
              if isSnapshot && !hasSnapshotRepo then // add snapshot repo to snapshot version
                Vector("//> using repository snapshots")
              else Vector.empty
            }

            os.write.over(path, newContent.mkString("\n") + "\n")
            println(s"Updated $path")
          }
      case "update" :: Nil =>
        println(s"Bumping Besom packages version to latest")
        val latestPackageVersions = fetchLatestPackageVersions
        projectFiles()
          .collect {
            case (path, content) if content.linesIterator.exists(besomDependencyPattern.matches) => path -> content
          }
          .foreachEntry { case (path, content) =>
            val newContent = content.linesIterator
              .map {
                case line if line.contains("besom-fake-") => line // ignore
                case besomDependencyPattern(prefix, version, suffix) =>
                  prefix + changeVersion(version, config.besomVersion, latestPackageVersions.get) + suffix
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

  private def projectFiles(path: os.Path = cwd): Map[os.Path, String] =
    val expectedFileNames = Vector("project.scala", "project-test.scala", "run.scala")
    println(s"Searching for project files in $path\n")
    os.walk(
      path,
      skip = (p: os.Path) => p.last == ".git" || p.last == ".out" || p.last == ".bsp" || p.last == ".scala-build" || p.last == ".idea"
    ).filter(f => expectedFileNames.contains(f.last))
      .map((f: os.Path) => f -> os.read(f))
      .toMap

  private def changeVersion(
    version: String,
    newBesomVersion: String,
    packageVersion: String => Option[String] = _ => None
  )(using config: Config): String =
    lazy val coreShortVersion = SemanticVersion
      .parseTolerant(config.besomVersion)
      .fold(
        e => throw Exception(s"Invalid besom version: ${config.besomVersion}", e),
        _.copy(patch = 0).toShortString
      )
    version match
      case s"$a:$b-core.$_" => s"$a:${packageVersion(a).getOrElse(b)}-core.$coreShortVersion"
      case s"$a:$_"         => s"$a:$newBesomVersion"
  end changeVersion

  def latestPackageVersion(name: String)(using Config): String =
    val latestPackageVersions = fetchLatestPackageVersions
    Try(latestPackageVersions(name)).recover { case e: NoSuchElementException =>
      throw Exception(s"package $name not found", e)
    }.get

  private def fetchLatestPackageVersions(using Config): Map[String, String] =
    println(s"Searching for latest package versions")
    given Flags = Flags()
    Packages
      .readOrFetchPackagesMetadata(Packages.packagesDir, Nil)
      .map { metadata =>
        metadata.name -> metadata.version.getOrElse(throw Exception("Package version must be present at this point")).asString
      }
      .toMap
  end fetchLatestPackageVersions

end Version
