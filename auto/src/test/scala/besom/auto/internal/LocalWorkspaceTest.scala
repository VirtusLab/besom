package besom.auto.internal

import besom.model.FullyQualifiedStackName
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
      prevRes    <- stack.preview()
      upRes      <- stack.up()
      destroyRes <- stack.destroy()
    yield (prevRes, upRes, destroyRes)
    res.fold(
      e => fail(e.getMessage, e),
      (prevRes, upRes, destroyRes) => {
        // Preview: summary (backward compat)
        assertEquals(prevRes.summary, Map(OpType.Create -> 1))

        // Preview: resourceChanges should contain ResourcePreEvent(s) for the create
        assert(prevRes.resourceChanges.nonEmpty, s"Expected non-empty resourceChanges, got: ${prevRes.resourceChanges}")
        assert(
          prevRes.resourceChanges.exists(_.metadata.op == OpType.Create),
          s"Expected at least one Create ResourcePreEvent, got ops: ${prevRes.resourceChanges.map(_.metadata.op)}"
        )

        // Preview: diagnostics is a List (may or may not be empty depending on provider)
        assert(prevRes.diagnostics != null)

        // Up: outputs (backward compat)
        assertEquals(
          upRes.outputs,
          Map(
            "exp_cfg" -> OutputValue(""),
            "exp_secret" -> OutputValue("", secret = true),
            "exp_static" -> OutputValue("foo")
          )
        )
        assertEquals(upRes.summary.kind, "update")
        assertEquals(
          upRes.summary.resourceChanges,
          Some(
            Map(
              OpType.Create.toString -> 1
            )
          )
        )
        assertEquals(upRes.summary.result, Some("succeeded"))

        // Up: resourceOperations should have ResOutputsEvent(s)
        assert(upRes.resourceOperations.nonEmpty, s"Expected non-empty resourceOperations, got: ${upRes.resourceOperations}")
        assert(upRes.failures.isEmpty, s"Expected no failures, got: ${upRes.failures}")

        // Destroy: should succeed without failures
        assertEquals(destroyRes.summary.result, Some("succeeded"))
        assert(destroyRes.failures.isEmpty, s"Expected no failures on destroy, got: ${destroyRes.failures}")
        assert(destroyRes.diagnostics != null)
      }
    )
  }
end LocalWorkspaceTest
