//> using scala "3.3.0"
//> using lib "com.lihaoyi::os-lib:0.9.1"

//> using dep "org.scalameta::munit::0.7.29"
//> using lib "com.lihaoyi::sourcecode:0.3.0"
//> using options "-java-output-version:11"

class LanguagePluginExecutorTest extends munit.FunSuite {
  val resourcesDir = os.Path(sourcecode.File()) / os.up / os.up / "resources"
  val executorsDir = resourcesDir / "executors"

  def getEnvVar(name: String) =
    sys.env.get(name).getOrElse(throw new Exception(s"Environment variable $name is not set"))

  def publishLocalResourcePlugin(pluginName: String) =
    val tmpBuildDir = os.temp.dir()
    os.list(resourcesDir / pluginName).foreach(file => os.copy.into(file, tmpBuildDir))
    os.proc("scala-cli", "--power", "publish", "local", ".").call(cwd = tmpBuildDir)

  def publishLocalResourcePlugins() =
    publishLocalResourcePlugin("fake-standard-resource-plugin")
    publishLocalResourcePlugin("fake-external-resource-plugin")

  def testExecutor(executorDir: os.Path) =
    val tmpPulumiHome        = os.temp.dir().toString
    val env                  = Map(
      "PULUMI_CONFIG_PASSPHRASE" -> "",
      "PULUMI_HOME" -> tmpPulumiHome
    )
    val stackName            = "organization/language-plugin-test/executor-test"
    val scalaPluginVersion   = "0.0.1-TEST"
    val scalaPluginLocalPath = getEnvVar("PULUMI_SCALA_PLUGIN_LOCAL_PATH")

    os.proc("pulumi", "login", s"file://${tmpPulumiHome}").call(env = env)
    os.proc(
      "pulumi",
      "plugin",
      "install",
      "language",
      "scala",
      scalaPluginVersion,
      "--file",
      scalaPluginLocalPath,
      "--reinstall"
    ).call(env = env)
    os.proc("pulumi", "stack", "--non-interactive", "init", stackName).call(cwd = executorDir, env = env)
    val pulumiUpOutput = os
      .proc("pulumi", "up", "--non-interactive", "--stack", stackName, "--skip-preview")
      .call(cwd = executorDir, env = env, check = false)
      .out
      .text()

    val expectedError = """java.lang.Exception: scala executor test got executed"""

    assert(clue(pulumiUpOutput).contains(expectedError))

    val aboutInfoLines = os.proc("pulumi", "about").call(cwd = executorDir, env = env).out.lines().toList

    val aboutPluginsVersions = aboutInfoLines
      .dropWhile(_ != "Plugins") // Find the plugins section
      .drop(2) // Drop headers
      .takeWhile(_.nonEmpty) // Empty line separates sections
      .map { line =>
        val lineParts = line.split("""\s+""") // Parse each plugin line splitting on whitespace
        lineParts(0) -> lineParts(1) // plugin_name -> plugin_version from extected format:   NAME    VERSION
      }
      .toMap

    val expectedAboutPluginsVersions = Map("scala" -> "unknown", "random" -> "4.3.1", "aci" -> "0.0.6")
    assert {
      clue(aboutInfoLines.mkString("\n"))
      clue(aboutPluginsVersions) == expectedAboutPluginsVersions
    }

    val pluginsLsLines = os.proc("pulumi", "plugin", "ls").call(cwd = executorDir, env = env).out.lines().toList
    val installedPluginsVersions = pluginsLsLines
      .drop(1) // Drop headers
      .takeWhile(_.nonEmpty) // Empty line separates sections
      .map { line =>
        val lineParts = line.split("""\s+""") // Parse each plugin line splitting on whitespace
        lineParts(0) -> lineParts(2) // plugin_name -> plugin_version from extected format:   NAME   KIND      VERSION     SIZE   INSTALLED   LAST USED
      }
      .toMap

    val expectedInstalledPluginsVersions = Map("scala" -> scalaPluginVersion, "random" -> "4.3.1", "aci" -> "0.0.6")

    assert {
      clue(pluginsLsLines.mkString("\n"))
      clue(installedPluginsVersions) == expectedInstalledPluginsVersions
    }

  override def beforeAll() =
    publishLocalResourcePlugins()

  override def afterAll() =
    os.proc("pulumi", "logout").call()

  // test("scala-cli") {
  //   // Prepare the sources of the test project
  //   val tmpProjectDir = os.temp.dir()
  //   os.list(executorsDir / "scala-cli").foreach(file => os.copy.into(file, tmpProjectDir))

  //   testExecutor(tmpProjectDir)
  // }

  test("sbt") {
    // Prepare the sources of the test project
    val tmpProjectDir = os.temp.dir()
    os.list(executorsDir / "sbt").foreach(file => os.copy.into(file, tmpProjectDir))

    testExecutor(tmpProjectDir)
  }

  // test("jar") {
  //   // Prepare the binary
  //   val tmpBuildDir = os.temp.dir()
  //   os.list(executorsDir / "scala-cli").foreach(file => os.copy.into(file, tmpBuildDir))
  //   os.proc("scala-cli", "--power", "package", ".", "--assembly", "-o", "app.jar").call(cwd = tmpBuildDir)

  //   // Prepare the sources of the test project
  //   val tmpProjectDir = os.temp.dir()
  //   os.list(executorsDir / "jar").foreach(file => os.copy.into(file, tmpProjectDir))
  //   os.copy.into(tmpBuildDir / "app.jar", tmpProjectDir)

  //   testExecutor(tmpProjectDir)
  // }
}
