package besom.scripts

import besom.codegen.Config
import org.apache.commons.lang3.SystemUtils
import os.*

object Proto:
  def main(args: String*): Unit =
    val cwd       = Config.besomDir
    val protoPath = cwd / "proto"
    args match
      case "fetch" :: Nil   => fetchProto(cwd, protoPath)
      case "compile" :: Nil => compileProto(cwd, protoPath)
      case "all" :: Nil =>
        fetchProto(cwd, protoPath)
        compileProto(cwd, protoPath)
        println("fetched & compiled")
      case "check" :: Nil =>
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
        println(s"Unknown command: $other")
        println("""Usage: proto <command>
            |
            |Commands:
            |  fetch   - fetches protobuf declaration files from pulumi repo(s)
            |  compile - compiles protobuf declaration files to scala code (codegen)
            |  all     - fetches & compiles
            |""".stripMargin)
        sys.exit(1)

private def fetchProto(cwd: os.Path, targetPath: os.Path): Unit =
  val pulumiRepoPath = cwd / "target" / "pulumi-proto"
  val pulumiRepo = sparseCheckout(
    pulumiRepoPath,
    "github.com/pulumi/pulumi.git",
    List(os.rel / "proto")
  )

  os.remove.all(targetPath)
  copyProto(pulumiRepo / "proto", targetPath)
  println("fetched protobuf declaration files")

private def copyProto(sourcePath: os.Path, targetPath: os.Path): Unit =
  println(s"copying from $sourcePath to $targetPath")

  val allowFileList = List(
    os.rel / "pulumi" / "alias.proto",
    os.rel / "pulumi" / "callback.proto",
    os.rel / "pulumi" / "engine.proto",
    os.rel / "pulumi" / "language.proto",
    os.rel / "pulumi" / "plugin.proto",
    os.rel / "pulumi" / "provider.proto",
    os.rel / "pulumi" / "resource.proto",
    os.rel / "pulumi" / "source.proto",
    os.rel / "pulumi" / "codegen" / "hcl.proto",
    os.rel / "google" / "protobuf" / "status.proto"
  )

  val allowExtensions = List("proto")

  os.makeDir.all(targetPath)
  os.walk(sourcePath)
    .filter(os.isFile(_))
    .filter(f => allowExtensions.contains(f.ext))
    .filter(f => allowFileList.exists(f.endsWith(_)))
    .foreach { source =>
      val target = targetPath / source.relativeTo(sourcePath)
      os.copy.over(source, target, createFolders = true, replaceExisting = true)
      println(s"copied ${source.relativeTo(sourcePath)} into ${target.relativeTo(targetPath)}")
    }

private def compileProto(cwd: os.Path, protoPath: os.Path): Unit =
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

private def fetchScalaProtocPlugin(cwd: os.Path): Unit =
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
