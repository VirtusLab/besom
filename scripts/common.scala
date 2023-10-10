import os.*

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

    val isCI  = sys.env.get("CI").contains("true")
    val token = sys.env.get("GITHUB_TOKEN")

    val url: String = (isCI, token) match {
      case (true, None)  => sys.error("Expected GITHUB_TOKEN environment variable to be defined in CI")
      case (_, Some(t))  => s"https://$t@$repoUrl"
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
