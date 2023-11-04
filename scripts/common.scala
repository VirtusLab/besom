import os.*

import java.time.LocalTime
import java.time.Duration
import java.time.temporal.Temporal
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec

def git(command: Shellable*)(wd: os.Path): CommandResult =
  val cmd = os.proc("git", command)
  println(s"[${wd.last}] " + cmd.commandChunks.mkString(" "))
  cmd.call(
    cwd = wd,
    stdin = os.Inherit,
    stdout = os.Inherit,
    stderr = os.Inherit
  )

def readLine: String = scala.io.StdIn.readLine

def isProtocInstalled: Boolean =
  scala.util.Try(os.proc("protoc").call()).isSuccess

def isUnzipAvailable: Boolean =
  scala.util.Try(os.proc("unzip", "-h").call()).isSuccess

def isGitInstalled: Boolean =
  scala.util.Try(os.proc("git", "version").call()).isSuccess

def sparseCheckout(
  repoPath: os.Path,
  repoUrl: String,
  allowDirList: List[os.RelPath]
): os.Path =
  var doClone = true
  if os.exists(repoPath) then
    print(s"'${repoPath.last}' cloned already, delete and checkout again? (y/N/q) ")
    @tailrec
    def loop(): Unit =
      readLine match
        case "y" | "Y"      => os.remove.all(repoPath)
        case "n" | "N" | "" => doClone = false
        case "q" | "Q"      => sys.exit(0)
        case _ =>
          print("(y/N/q) ")
          loop()
    loop()

  if doClone then
    if !isGitInstalled then
      println("You need git installed for this to work!")
      sys.exit(1)

    val url: String = githubToken match {
      case Some(t) => s"https://$t@$repoUrl"
      case None    => s"https://$repoUrl"
    }

    os.remove.all(repoPath)
    os.makeDir.all(repoPath / os.up)
    git(
      "clone",
      "--filter=tree:0",
      "--no-checkout",
      "--single-branch",
      "--depth=1",
      "--no-tags",
      "--shallow-submodules",
      "--sparse",
      "--",
      url,
      repoPath.last
    )(
      repoPath / os.up
    )
    git("sparse-checkout", "set", allowDirList)(repoPath)
    git("checkout")(repoPath)

    println(s"cloned git repo of pulumi to $repoPath")

  repoPath

def copyFilteredFiles(
  sourcePath: Path,
  targetPath: Path,
  allowDirList: List[String],
  allowFileList: List[String],
  allowExtensions: List[String]
): Unit =
  os.makeDir.all(targetPath)
  os.walk(sourcePath)
    .filter(os.isFile(_))
    .filter(f => allowExtensions.contains(f.ext))
    .filter {
      case _ / d / _ if allowDirList.contains(d) => true
      case _ / f if allowFileList.contains(f)    => true
      case _                                     => false
    }
    .foreach { source =>
      val target = targetPath / source.relativeTo(sourcePath)
      os.copy.over(source, target, createFolders = true, replaceExisting = true)
      println(s"copied ${source.relativeTo(sourcePath)} into ${target.relativeTo(targetPath)}")
    }

def isCI: Boolean = sys.env.get("CI").contains("true")

def githubToken: Option[String] =
  val token = sys.env.get("GITHUB_TOKEN")
  (isCI, token) match
    case (true, None) => sys.error("Expected GITHUB_TOKEN environment variable to be defined in CI")
    case (_, t)       => t // None is usually fine, we'll just use the public API

class Progress(
  var label: String,
  val report: (String, Int, Temporal) => Unit,
  val summary: (String, Temporal, Temporal) => Unit,
  val failure: (String, String) => Unit,
  var time: Temporal = LocalTime.now()
):
  private def increment(
    amount: Int = 1,
    lbl: String = label,
    tm: Temporal = LocalTime.now()
  ): Unit =
    label = lbl
    time = tm
    report(lbl, amount, tm)

  private def end(end: Temporal = LocalTime.now()): Unit =
    summary(label, time, end)

  private def fail(error: String): Unit =
    failure(label, error)

object Progress:
  def report(using progress: Progress): Unit =
    progress.increment()

  def report(amount: Int)(using progress: Progress): Unit =
    progress.increment(amount)

  def report(label: String)(using progress: Progress): Unit =
    progress.increment(lbl = label)

  def report(amount: Int, label: String)(using progress: Progress): Unit =
    progress.increment(amount, lbl = label)

  def failure(error: String)(using progress: Progress): Unit =
    progress.fail(error)

  def summary(using progress: Progress): Unit =
    progress.end()

def withProgress[A](title: String, total: Int)(f: Progress ?=> A): A =
  val counter = new AtomicInteger(0)
  val failed  = collection.concurrent.TrieMap.empty[String, String]
  val first   = LocalTime.now()

  def elapsed(from: Temporal, to: Temporal = LocalTime.now()): String = {
    Duration.between(from, to).toSeconds match
      case s if s < 60 => s"$s seconds"
      case s           => s"${s / 60} minutes"
  }

  // noinspection ScalaUnusedSymbol
  val progress = Progress(
    title,
    report = (lbl, amount, time) =>
      val current    = counter.addAndGet(amount)
      val percentage = (current.toDouble / total * 100).toInt
      Console.out.synchronized {
        println(s"\r$lbl: $percentage% [$current/$total] [${time}]")
      }
    ,
    summary = (lbl, start, end) =>
      if failed.contains(lbl) then
        Console.out.synchronized {
          println(s"\r$lbl: ERROR [${elapsed(start, end)}]")
        }
      else
        Console.out.synchronized {
          println(s"\r$lbl: DONE [${elapsed(start, end)}]")
        }
    ,
    failure = (lbl, error) =>
      failed.put(lbl, error)
  )

  println(title)

  try f(using progress)
  finally
    println(s"Total time: ${elapsed(first)}")
    if failed.nonEmpty then
      println(s"Failures [${failed.size}]:")
      failed.foreach((name, error) => println(s"  - $name: $error"))
    println()
