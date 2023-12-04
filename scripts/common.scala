import os.*

import java.time.LocalTime
import java.time.temporal.Temporal
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.Duration

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

end sparseCheckout

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
  val ending: (String, Temporal, Temporal) => Unit,
  val failure: (String, Temporal, Temporal, String) => Unit,
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

  private def end(endTime: Temporal = LocalTime.now()): Unit =
    ending(label, time, endTime)

  private def fail(error: String, end: Temporal = LocalTime.now()): Unit =
    failure(label, time, end, error)

object Progress:
  def report(using progress: Progress): Unit =
    progress.increment()

  def report(amount: Int)(using progress: Progress): Unit =
    progress.increment(amount)

  def report(label: String)(using progress: Progress): Unit =
    progress.increment(lbl = label)

  def report(amount: Int, label: String)(using progress: Progress): Unit =
    progress.increment(amount, lbl = label)

  def fail(error: String)(using progress: Progress): Unit =
    progress.fail(error)

  def end(using progress: Progress): Unit =
    progress.end()

def withProgress[A](title: String, total: Int)(f: Progress ?=> A): A =
  val counter   = new AtomicInteger(0)
  val failed    = collection.concurrent.TrieMap.empty[String, String]
  val succeeded = collection.concurrent.TrieMap.empty[String, String]
  val first     = LocalTime.now()

  def elapsed(from: Temporal, to: Temporal = LocalTime.now()): String =
    Duration.fromNanos(java.time.Duration.between(from, to).toNanos) match
      case d if d.toHours > 0   => s"${d.toHours} h ${d.toMinutes % 60} m ${d.toSeconds % 60} s"
      case d if d.toMinutes > 0 => s"${d.toMinutes} m ${d.toSeconds % 60} s"
      case d if d.toSeconds > 0 => s"${d.toSeconds} s ${d.toMillis % 1000} ms"
      case d if d.toMillis > 0  => s"${d.toMillis} ms ${d.toMicros % 1000} µs"
      case d                    => s"${d.toMicros} µs"

  def end(lbl: String, start: Temporal, end: Temporal) =
    if !failed.contains(lbl)
    then succeeded.put(lbl, s"\r$lbl: DONE [${elapsed(start, end)}]")
  def failure(lbl: String, start: Temporal, end: Temporal, error: String) = 
    failed.put(lbl, s"\r$lbl: ERROR [${elapsed(start, end)}]: $error")

  def report(lbl: String, amount: Int, time: Temporal): Unit =
    val current = counter.addAndGet(amount)
    val percentage = (current.toDouble / total * 100).toInt
    Console.out.synchronized {
      println(s"\r$lbl: $percentage% [$current/$total] [${time}]")
    }

  // noinspection ScalaUnusedSymbol
  val progress = Progress(title, report = report, ending = end, failure = failure)

  println(title)

  try f(using progress)
  finally
    println()
    println(s"Successes [${succeeded.size}/$total]:")
    succeeded.toVector.sortBy(_._1).foreach((name, msg) => println(s"  - $name: $msg"))
    println()
    if failed.nonEmpty then
      println(s"Failures [${failed.size}/$total]:")
      failed.toVector.sortBy(_._1).foreach((name, error) => println(s"  - $name: $error"))
    println()
    println(s"Total [${succeeded.size}/$total] time: ${elapsed(first)}")
    println()

end withProgress
