package besom.auto

import besom.FullyQualifiedStackName
import besom.test.*
import besom.util.eitherOps

class LocalWorkspaceTest extends munit.FunSuite:

  FunFixture[FullyQualifiedStackName](
    setup = t => fqsn(this.getClass, t),
    teardown = _ => ()
  ).test("new stack remote source with setup") { generatedStackName =>
    val stackName     = FullyQualifiedStackName("goproj", generatedStackName.stack)
    val pulumiHomeDir = os.temp.dir() / ".pulumi"
    loginLocal(pulumiHomeDir)

    val binName = "examplesBinary"
    val bin     = if System.getProperty("os.name").startsWith("Windows") then binName + ".exe" else binName
    val binaryBuilder = (ws: Workspace) => {
      shell("go", "build", "-o", bin, "main.go")(shell.ShellOption.Cwd(ws.workDir)).bimap(
        e => e.withMessage("go build failed"),
        _ => ()
      )
    }

    val res = for
      stack <- createStackRemoteSource(
        stackName,
        GitRepo(
          url = "https://github.com/pulumi/test-repo.git",
          projectPath = "goproj",
          setup = binaryBuilder
        ),
        LocalWorkspaceOption.Project(
          Project(
            name = "goproj",
            runtime = ProjectRuntimeInfo(
              name = "go",
              options = Map("binary" -> binName)
            )
          )
        ),
        LocalWorkspaceOption.PulumiHome(pulumiHomeDir),
        LocalWorkspaceOption.EnvVars(shell.pulumi.env.PulumiConfigPassphraseEnv -> "test")
      )
      prevRes <- stack.preview()
      upRes   <- stack.up()
    yield (prevRes, upRes)
    res.fold(
      e => fail(e.getMessage, e),
      (prevRes, upRes) => {
        println(prevRes)
        println(upRes)
      }
    )
  }
end LocalWorkspaceTest
