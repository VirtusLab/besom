//> using scala "3.2.0"

//> using lib "com.lihaoyi::os-lib:0.8.1"
//> using lib "com.lihaoyi::requests:0.7.0"
//> using lib "com.lihaoyi::upickle:2.0.0"
//> using lib "org.apache.commons:commons-lang3:3.12.0"

import org.apache.commons.lang3.SystemUtils

def readLine = scala.io.StdIn.readLine

@main def proto(command: String): Unit =
    val cwd = os.pwd
    if cwd.last != "besom" then
        println("You have to run this command from besom project root directory!")
        sys.exit(1)

    os.makeDir.all(cwd / "target")    

    command match
        case "fetch" => fetch(cwd)
        case "compile" => compile(cwd)    
        case "all" =>
            fetch(cwd)
            compile(cwd)
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

def isProtocInstalled: Boolean = 
    scala.util.Try(os.proc("protoc").call()).isSuccess

def isUnzipAvailable: Boolean = 
    scala.util.Try(os.proc("unzip", "-h").call()).isSuccess

def isGitInstalled: Boolean =
    scala.util.Try(os.proc("git", "version").call()).isSuccess

def fetch(cwd: os.Path): Unit = 
    val targetPath = cwd / "target" / "pulumi"

    var doClone = true
    if os.exists(targetPath) then
        print("cloned already, delete and clone again? (y/n/q) ")
        def loop: Unit =
            readLine match 
                case "y" => os.remove.all(targetPath)
                case "n" => doClone = false
                case "q" => sys.exit(0)
                case _ => loop
        loop
    
    if doClone then
        if !isGitInstalled then
            println("You need git installed for this to work!")
            sys.exit(1)

        val token = sys.env.getOrElse("TOKEN", sys.error("Access Token has to be defined!"))

        os.proc("git", "clone", s"https://$token@github.com/pulumi/pulumi.git", targetPath).call(
            stdin = os.Inherit, 
            stdout = os.Inherit, 
            stderr = os.Inherit
        )
        println(s"cloned git repo of pulumi to $targetPath")

    os.makeDir.all(cwd / "proto")
    os.walk.stream.attrs(targetPath / "sdk" / "proto", skip = (_, attrs) => attrs.isDir).foreach { case (f, _) =>
        os.copy.into(f, cwd / "proto", replaceExisting = true)
        println(s"copied ${f.last} into ${cwd / "proto"}")
    }

    println("fetched protobuf declaration files!")

def compile(cwd: os.Path): Unit =
    if !isProtocInstalled then
        println("You need protoc protobuffer compiler installed for this to work!")
        sys.exit(1)

    if !isUnzipAvailable then
        println("You need unzip installed for this to work!")
        sys.exit(1)

    if !os.exists(cwd / "proto") then
        println("run `scala-cli run ./scripts -M proto -- fetch` first!")

    if !os.exists(cwd / "target" / "protoc-gen-scala") then
        fetchScalaProtocPlugin(cwd)

    val outputPath = cwd / "src" / "main" / "scala" / "besom" / "rpc"

    val files = os.list(cwd / "proto").map(_.last).filter(_.endsWith("proto"))
    
    os.makeDir.all(outputPath)

    // this generates GRPC netty-based impl for Scala in ./src/main/scala/besom/rpc, notice grpc: in --scala_out 
    os.proc("protoc", files, s"--plugin=${cwd / "target" / "protoc-gen-scala"}", "--scala_out=grpc:../src/main/scala/besom/rpc").call(cwd / "proto")    
    
    println(s"generated scala code from protobuf declaration files to $outputPath")    

def fetchScalaProtocPlugin(cwd: os.Path): Unit = 
    val releasesJsonBodyStr = requests.get("https://api.github.com/repos/scalapb/ScalaPB/releases")
    val json = ujson.read(releasesJsonBodyStr)
    val latestVersion = json.arr.headOption.getOrElse(throw Exception("No releases of ScalaPB found, check https://github.com/scalapb/ScalaPB/releases"))
    val assets = latestVersion.obj("assets")
    val currentOS = if SystemUtils.IS_OS_MAC then "osx" else "linux"
    val releases = assets.arr.map(_.obj("name").str).mkString(", ")
    println(s"found these latest protoc-gen-scala plugin releases: $releases")
    val downloadUrl = 
        assets.arr
            .find(_.obj("name").str.contains(currentOS))
            .map(_.obj("browser_download_url").str)
            .getOrElse {
                val releases = assets.arr.map(_.obj("name").str).mkString(", ")
                throw Exception(s"Unable to find scalapbc plugin for this OS, current os: $currentOS, found releases: $releases!") 
            }

    println(s"fetching $downloadUrl")    

    os.write.over(cwd / "target" / "protoc-gen-scala.zip", requests.get.stream(downloadUrl))

    os.proc("unzip", "-o", cwd / "target" / "protoc-gen-scala.zip").call(cwd = cwd / "target")

    os.remove(cwd / "target" / "protoc-gen-scala.zip")
    