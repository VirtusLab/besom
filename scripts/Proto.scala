//> using scala "3.3.1"

//> using lib "com.lihaoyi::os-lib:0.8.1"
//> using lib "com.lihaoyi::requests:0.8.0"
//> using lib "com.lihaoyi::upickle:3.1.3"
//> using lib "org.apache.commons:commons-lang3:3.13.0"

import org.apache.commons.lang3.SystemUtils
import os.*

import scala.annotation.tailrec

@main def proto(command: String): Unit =
  val cwd = os.pwd
  if cwd.last != "besom" then
    println("You have to run this command from besom project root directory!")
    sys.exit(1)

  val protoPath = cwd / "proto"
  command match
    case "fetch" => fetch(cwd, protoPath)
    case "compile" => compile(cwd, protoPath)
    case "all" =>
      fetch(cwd, protoPath)
      compile(cwd, protoPath)
      println("fetched & compiled")

    case "check" =>
      if !isProtocInstalled then
        println("You need `protoc` protobuffer compiler installed for this to work!")
        sys.exit(1)
      if !isGitInstalled then
        println("You need `git` installed for this to work!")
        sys.exit(1)
      if !isUnzipAvailable then
        println("You need `unzip` installed for this to work!")
        sys.exit(1)

    case other =>
      println(s"unknown command: $other")
      sys.exit(1)

// TODO: de-duplicate with Schemas.scala
private def readLine: String = scala.io.StdIn.readLine

def isProtocInstalled: Boolean =
  scala.util.Try(os.proc("protoc").call()).isSuccess

def isUnzipAvailable: Boolean =
  scala.util.Try(os.proc("unzip", "-h").call()).isSuccess

def isGitInstalled: Boolean =
  scala.util.Try(os.proc("git", "version").call()).isSuccess

private def fetch(cwd: os.Path, targetPath: os.Path): Unit =
  val pulumiRepoPath = cwd / "target" / "pulumi-proto"
  val pulumiRepo = sparseCheckout(
    pulumiRepoPath,
    "github.com/pulumi/pulumi.git",
    List(os.rel / "proto")
  )

  os.remove.all(targetPath)
  copy(pulumiRepo / "proto", targetPath)
  println("fetched protobuf declaration files")

// TODO: de-duplicate with Schemas.scala
private def sparseCheckout(
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
        case "y" | "Y" => os.remove.all(repoPath)
        case "n" | "N" | "" => doClone = false
        case "q" | "Q" => sys.exit(0)
        case _ =>
          print("(y/N/q) ")
          loop()
    loop()

  if doClone then
    if !isGitInstalled then
      println("You need git installed for this to work!")
      sys.exit(1)

    val isCI  = sys.env.get("CI").contains("true")
    val token = sys.env.get("GITHUB_TOKEN")

    val url: String = (isCI, token) match {
      case (true, None) => sys.error("Expected GITHUB_TOKEN environment variable to be defined in CI")
      case (_, Some(t)) => s"https://$t@$repoUrl"
      case (false, None) => s"https://$repoUrl"
    }

    os.remove.all(repoPath)
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

private def copy(sourcePath: os.Path, targetPath: os.Path): Unit =
  println(s"copying from $sourcePath to $targetPath")

  val allowDirList = List()

  val allowFileList = List(
    "alias.proto",
    "engine.proto",
    "plugin.proto",
    "provider.proto",
    "resource.proto",
    "source.proto",
    "status.proto"
  )

  os.makeDir.all(targetPath)
  os.walk(sourcePath)
    .filter(os.isFile(_))
    .filter(_.ext == "proto")
    .filter {
      case _ / d / _ if allowDirList.contains(d) => true
      case _ / f if allowFileList.contains(f)    => true
      case _                                     => false
    }
    .foreach { source =>
      val target = targetPath / source.relativeTo(sourcePath)
      os.copy.over(source, target, replaceExisting = true, createFolders = true)
      println(s"copied ${source.relativeTo(sourcePath)} into ${target.relativeTo(targetPath)}")
    }

private def compile(cwd: os.Path, protoPath: os.Path): Unit =
  if !isProtocInstalled then
    println("You need protoc protobuffer compiler installed for this to work!")
    sys.exit(1)

  if !isUnzipAvailable then
    println("You need unzip installed for this to work!")
    sys.exit(1)

  if !os.exists(protoPath) then println("run `scala-cli run ./scripts -M proto -- fetch` first!")

  val pluginPath = cwd / "target" / "protoc-gen-scala"
  if !os.exists(pluginPath) then fetchScalaProtocPlugin(cwd)

  val files = os.walk(protoPath).filter(_.ext == "proto")

  val scalaOut = cwd / "core" / "src" / "main" / "scala" / "besom" / "rpc"
  os.remove.all(scalaOut)
  os.makeDir.all(scalaOut)

  // this generates GRPC netty-based impl for Scala in ./src/main/scala/besom/rpc, notice grpc: in --scala_out
  val protoc = os.proc(
    "protoc",
    s"--plugin=$pluginPath",
    s"--scala_out=grpc:$scalaOut",
    files.map(_.relativeTo(protoPath))
  )
  println(s"running [$protoPath]: ${protoc.commandChunks.mkString(" ")}")
  protoc.call(
    protoPath,
    stdin = os.Inherit,
    stdout = os.Inherit,
    stderr = os.Inherit
  )

  println(s"generated scala code from protobuf declaration files to $scalaOut")

def fetchScalaProtocPlugin(cwd: os.Path): Unit =
  val releasesJsonBodyStr = requests.get("https://api.github.com/repos/scalapb/ScalaPB/releases")
  val json                = ujson.read(releasesJsonBodyStr)
  val latestVersion = json.arr.headOption.getOrElse(
    throw Exception("No releases of ScalaPB found, check https://github.com/scalapb/ScalaPB/releases")
  )
  val assets    = latestVersion.obj("assets")
  val currentOS = if SystemUtils.IS_OS_MAC then "osx" else "linux"
  val releases  = assets.arr.map(_.obj("name").str).mkString(", ")
  println(s"found these latest protoc-gen-scala plugin releases: $releases")
  val downloadUrl =
    assets.arr
      .find(_.obj("name").str.contains(currentOS))
      .map(_.obj("browser_download_url").str)
      .getOrElse {
        val releases = assets.arr.map(_.obj("name").str).mkString(", ")
        throw Exception(
          s"Unable to find scalapbc plugin for this OS, current os: $currentOS, found releases: $releases!"
        )
      }

  println(s"fetching $downloadUrl")

  // unzip the plugin
  os.write.over(cwd / "target" / "protoc-gen-scala.zip", requests.get.stream(downloadUrl))
  os.proc("unzip", "-o", cwd / "target" / "protoc-gen-scala.zip").call(cwd = cwd / "target")
  os.remove(cwd / "target" / "protoc-gen-scala.zip")

// TODO: de-duplicate with Schemas.scala
private def git(command: Shellable*)(wd: os.Path) =
  val cmd = os.proc("git", command)
  println(s"[${wd.last}] " + cmd.commandChunks.mkString(" "))
  cmd.call(
    cwd = wd,
    stdin = os.Inherit,
    stdout = os.Inherit,
    stderr = os.Inherit
  )