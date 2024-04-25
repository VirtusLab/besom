package besom.scripts

import os.*

import java.time.LocalTime
import java.time.temporal.Temporal
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.Duration

import besom.codegen.Config

object Args:
  def parse(args: Seq[String], monoFlags: Seq[String] = Seq.empty): (Vector[String], Map[String, Int | String]) =
    val (rest, flags) = parseInner(args, monoFlags)
    val mergedFlags: Map[String, Int | String] = flags.groupMapReduce(_._1)(_._2) {
      case (a: Int, b: Int)       => a + b
      case (a: Int, b: String)    => a + Integer.parseInt(b)
      case (a: String, b: Int)    => Integer.parseInt(a) + b
      case (a: String, b: String) => b // last one wins
    }
    (rest, mergedFlags)

  private def parseInner(args: Seq[String], monoFlags: Seq[String]): (Vector[String], Vector[(String, Int | String)]) =
    args match
      case Nil => (Vector.empty, Vector.empty)
      case v +: tail if monoFlags.contains(s"--$v") || monoFlags.contains(s"-$v") =>
        val (rest, flags) = parseInner(tail, monoFlags)
        (rest, (v, 1) +: flags)
      case s"--$name" +: value +: tail =>
        val (rest, flags) = parseInner(tail, monoFlags)
        (rest, (name, value) +: flags)
      case s"-$name" +: value +: tail =>
        val (rest, flags) = parseInner(tail, monoFlags)
        (rest, (name, value) +: flags)
      case head +: tail =>
        val (rest, flags) = parseInner(tail, monoFlags)
        (head +: rest, flags)

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

lazy val isCI: Boolean =
  val ci = sys.env.get("CI").contains("true")
  println(s"CI: ${ci}")
  ci

def githubToken: Option[String] =
  val token = sys.env.get("GITHUB_TOKEN")
  (isCI, token) match
    case (true, None) => sys.error("Expected GITHUB_TOKEN environment variable to be defined in CI")
    case (_, t)       => t // None is usually fine, we'll just use the public API

def envOrExit(key: String): String =
  sys.env.getOrElse(
    key, {
      System.err.println(s"\nExpected '$key' environment variable to be set\n")
      sys.exit(1)
    }
  )

class Progress(
  var label: String,
  val report: (String, Int, Temporal) => Unit,
  val ending: (String, Temporal, Temporal) => Unit,
  val failure: (String, Temporal, Temporal, String) => Unit,
  val total: Int => Unit,
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

  private def updateTotal(amount: Int): Unit =
    total(amount)

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

  def total(amount: Int)(using progress: Progress): Unit = progress.updateTotal(amount)

def withProgress[A](title: String, initialTotal: Int)(f: Progress ?=> A): A =
  val counter   = AtomicInteger(0)
  val failed    = collection.concurrent.TrieMap.empty[String, String]
  val succeeded = collection.concurrent.TrieMap.empty[String, String]
  val first     = LocalTime.now()
  val realTotal = AtomicInteger(initialTotal)

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

  def total(amount: Int): Unit = realTotal.getAndUpdate(Math.max(_, amount))

  def report(lbl: String, amount: Int, time: Temporal): Unit =
    val current    = counter.addAndGet(amount)
    val percentage = (current.toDouble / realTotal.get() * 100).toInt
    Console.out.synchronized {
      println(s"\r$lbl: $percentage% [$current/${realTotal.get()}] [${time}]")
    }

  // noinspection ScalaUnusedSymbol
  val progress = Progress(title, report = report, ending = end, failure = failure, total = total)

  println(title)

  try f(using progress)
  finally
    println()
    println(s"Successes [${succeeded.size}/${realTotal.get()}]:")
    succeeded.toVector.sortBy(_._1).foreach((name, msg) => println(s"  - $name: $msg"))
    println()
    if failed.nonEmpty then
      println(s"Failures [${failed.size}/${realTotal.get()}]:")
      failed.toVector.sortBy(_._1).foreach((name, error) => println(s"  - $name: $error"))
    println()
    println(s"Total [${succeeded.size}/${realTotal.get()}] time: ${elapsed(first)}")
    println()

end withProgress

import coursier.Repository

def resolveMavenPackageVersion(name: String)(using config: Config): Either[Exception, String] =
  import coursier.Repositories
  resolvePackageVersion(
    name,
    config.scalaVersion,
    Vector(Repositories.central, Repositories.sonatype("public"), Repositories.sonatype("snapshots"))
  )
end resolveMavenPackageVersion

def resolveLocalPackageVersion(name: String)(using config: Config): Either[Exception, String] =
  resolvePackageVersion(
    name,
    config.scalaVersion,
    Vector()
  )
end resolveLocalPackageVersion

private def resolvePackageVersion(name: String, defaultScalaVersion: String, repositories: Vector[Repository]): Either[Exception, String] =
  import coursier.*
  import coursier.parse.DependencyParser
  import scala.concurrent.ExecutionContext.Implicits.global

  DependencyParser
    .dependency(name, defaultScalaVersion)
    .fold(
      msg => Left(Exception(s"Failed to parse $name: $msg")),
      Right(_)
    )
    .flatMap { dep =>
      Resolve()
        .addRepositories(repositories*)
        .addDependencies(dep)
        .either()
        .flatMap {
          _.reconciledVersions.get(dep.module).toRight(Exception(s"Failed to resolve $name"))
        }
    }
end resolvePackageVersion
