//> using scala 3.3.5
//> using toolkit default

import os.*
import scala.util.Try

object UpdateDependencies {

  def main(args: Array[String]): Unit = {
    val cwd           = os.pwd
    val publishedFile = cwd / "all-published.txt"

    if (!os.exists(publishedFile)) {
      println(s"Error: File '$publishedFile' not found.")
      return
    }

    val publishedDeps: Map[(String, String), String] = parsePublishedFile(publishedFile)

    val projectFiles = os
      .walk(cwd)
      .filter(f => f.last == "project.scala" && os.isFile(f))
      .toVector

    println(s"Found ${projectFiles.size} project.scala files.")

    projectFiles.foreach { file =>
      updateProjectFile(file, publishedDeps)
    }
  }

  /** Parses `published.txt` into a map of (organization, artifact) -> version */
  def parsePublishedFile(file: Path): Map[(String, String), String] = {
    os.read
      .lines(file)
      .flatMap { line =>
        val parts = line.split("::", 2)
        if (parts.length == 2) {
          val org             = parts(0).trim
          val artifactVersion = parts(1).split(":", 2)
          if (artifactVersion.length == 2) {
            val artifact = artifactVersion(0).trim
            val version  = artifactVersion(1).trim
            Some((org, artifact) -> version)
          } else None
        } else None
      }
      .toMap
  }

  /** Updates `project.scala` file by replacing outdated dependencies */
  def updateProjectFile(file: Path, publishedDeps: Map[(String, String), String]): Unit = {
    val originalLines = os.read.lines(file)

    val updatedLines = originalLines.map { line =>
      if (line.startsWith("//> using dep ")) {
        println(s"found dep `$line`")
        val depPattern = """//> using dep\s+(?:"|)([^:]+)::([^:]+):([^"\s]+)(?:"|)""".r
        line match {
          case depPattern(org, artifact, oldVersion) =>
            publishedDeps.get((org, artifact)) match {
              case Some(newVersion) if newVersion != oldVersion =>
                println(s"Updating $artifact from $oldVersion to $newVersion in ${file.relativeTo(os.pwd)}")
                s"""//> using dep "$org::$artifact:$newVersion""""
              case _ => line
            }
          case _ =>
            println(s"Non-scala dep format: $line")
            line
        }
      } else line
    }

    if (originalLines != updatedLines) {
      os.write.over(file, updatedLines.mkString("\n") + "\n")
    }
  }
}
