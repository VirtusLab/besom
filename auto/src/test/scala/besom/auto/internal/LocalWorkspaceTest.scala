package besom.auto.internal

import besom.model.FullyQualifiedStackName
import besom.test.*
import besom.util.eitherOps

import scala.concurrent.duration.*

class LocalWorkspaceTest extends munit.FunSuite:

  // these shell out to git, go and pulumi and download plugins, which does not fit in munit's 30s default
  override def munitTimeout: Duration = 5.minutes

  /** Collects the engine events delivered by `OnEvent`, keeping enough timing to tell live delivery from a post-hoc dump. */
  private class EventRecorder:
    private val events                         = scala.collection.mutable.ListBuffer.empty[EngineEvent]
    @volatile private var firstEventAt: Long   = -1L
    @volatile private var callReturnedAt: Long = -1L

    val record: EngineEvent => Unit = e =>
      events.synchronized {
        if firstEventAt < 0 then firstEventAt = System.nanoTime()
        events += e
      }

    /** Runs `call`, stamping the moment it returned - on the failure path too, so liveness stays checkable there. */
    def timing[A](call: => A): A =
      try call
      finally callReturnedAt = System.nanoTime()

    def all: List[EngineEvent]            = events.synchronized(events.toList)
    def preEvents: List[ResourcePreEvent] = all.flatMap(_.resourcePreEvent)
    def firstArrivedBeforeReturn: Boolean = firstEventAt > 0 && callReturnedAt > 0 && firstEventAt < callReturnedAt
  end EventRecorder

  /** The explicit parent URN of a step, empty for the root stack resource. */
  private def parentOf(meta: StepEventMetadata): Option[String] = meta.`new`.orElse(meta.old).map(_.parent)

  FunFixture[FullyQualifiedStackName](
    setup = t => fqsn(this.getClass, t),
    teardown = _ => ()
  ).test("inline source carries its program to the workspace") { generatedStackName =>
    val pulumiHomeDir = os.temp.dir() / ".pulumi"
    loginLocal(pulumiHomeDir)

    // never executed - nothing in besom-auto runs an inline program, the workspace only carries it
    val program: RunFunc    = _ => ???
    val ignoredOpt: RunFunc = _ => ???

    val res = createStackInlineSource(
      FullyQualifiedStackName("inlineproj", generatedStackName.stack),
      "inlineproj",
      program,
      // a Program in opts must lose to the explicit program argument, whichever order the options are merged in
      LocalWorkspaceOption.Program(ignoredOpt),
      LocalWorkspaceOption.PulumiHome(pulumiHomeDir),
      LocalWorkspaceOption.EnvVars(shell.pulumi.env.PulumiConfigPassphraseEnv -> "test")
    )

    res.fold(
      e => fail(e.getMessage, e),
      stack => assertEquals(stack.workspace.program, Some(program))
    )
  }

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

    val upEvents      = EventRecorder()
    val upAgainEvents = EventRecorder()

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
      upRes   <- upEvents.timing(stack.up(UpOption.OnEvent(upEvents.record)))
      // a second, no-op up is the only way to observe `same` steps, which the "full tree" premise depends on
      upAgainRes <- upAgainEvents.timing(stack.up(UpOption.OnEvent(upAgainEvents.record)))
      destroyRes <- stack.destroy()
    yield (prevRes, upRes, upAgainRes, destroyRes)
    res.fold(
      e => fail(e.getMessage, e),
      (prevRes, upRes, upAgainRes, destroyRes) => {
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

        // Up: resourcePreEvents restores the *start* of each operation
        assert(upRes.resourcePreEvents.nonEmpty, s"Expected non-empty resourcePreEvents, got: ${upRes.resourcePreEvents}")
        assertEquals(upRes.parseErrors, Nil)

        // Destroy: should succeed without failures
        assertEquals(destroyRes.summary.result, Some("succeeded"))
        assert(destroyRes.failures.isEmpty, s"Expected no failures on destroy, got: ${destroyRes.failures}")
        assert(destroyRes.diagnostics != null)
        assert(destroyRes.resourcePreEvents.nonEmpty, s"Expected non-empty resourcePreEvents on destroy")
        assertEquals(destroyRes.parseErrors, Nil)

        // ── live event delivery ─────────────────────────────────────────
        val live = upEvents.all
        assert(live.nonEmpty, "no engine events were delivered live")

        // (a) liveness - the first event has to arrive while up is still running
        assert(upEvents.firstArrivedBeforeReturn, "the first engine event did not arrive before up returned")

        // the live stream must agree with the post-hoc parse, that is what makes it a drop-in replacement
        assertEquals(live.flatMap(_.resOutputsEvent).map(_.metadata.urn), upRes.resourceOperations.map(_.metadata.urn))
        assertEquals(upEvents.preEvents.map(_.metadata.urn), upRes.resourcePreEvents.map(_.metadata.urn))

        // (b) `same` steps are present in the log - it precedes Pulumi's display filter
        val sameSteps = upAgainEvents.preEvents.filter(_.metadata.op == OpType.Same)
        assertEquals(upAgainRes.summary.result, Some("succeeded"))
        assert(
          sameSteps.nonEmpty,
          s"Expected `same` steps in a no-op up, got ops: ${upAgainEvents.preEvents.map(_.metadata.op)}"
        )

        // (c) the root stack resource carries an explicit, empty parent
        // (this program has no other resources - the nested case is covered by the resource tree test below)
        val roots = upEvents.preEvents.filter(_.metadata.`type` == "pulumi:pulumi:Stack")
        assert(roots.nonEmpty, "no pulumi:pulumi:Stack ResourcePreEvent")
        assertEquals(roots.flatMap(e => parentOf(e.metadata)).distinct, List(""))
      }
    )
  }
  FunFixture[FullyQualifiedStackName](
    setup = t => fqsn(this.getClass, t),
    teardown = _ => ()
  ).test("engine events describe the full resource tree") { generatedStackName =>
    val stackName     = FullyQualifiedStackName("goproj", generatedStackName.stack)
    val pulumiHomeDir = os.temp.dir() / ".pulumi"
    loginLocal(pulumiHomeDir)

    // component resources need no provider plugin, so a three level tree costs nothing beyond the SDK the fixture already vendors
    val program =
      """|package main
         |
         |import "github.com/pulumi/pulumi/sdk/v3/go/pulumi"
         |
         |type Group struct{ pulumi.ResourceState }
         |
         |func main() {
         |	pulumi.Run(func(ctx *pulumi.Context) error {
         |		parent := &Group{}
         |		if err := ctx.RegisterComponentResource("besom:test:Group", "parent", parent); err != nil {
         |			return err
         |		}
         |		child := &Group{}
         |		if err := ctx.RegisterComponentResource("besom:test:Group", "child", child, pulumi.Parent(parent)); err != nil {
         |			return err
         |		}
         |		grandchild := &Group{}
         |		if err := ctx.RegisterComponentResource("besom:test:Group", "grandchild", grandchild, pulumi.Parent(child)); err != nil {
         |			return err
         |		}
         |		return nil
         |	})
         |}
         |""".stripMargin

    val binName = "treeBinary"
    val bin     = if System.getProperty("os.name").startsWith("Windows") then binName + ".exe" else binName
    val binaryBuilder = (ws: Workspace) => {
      os.write.over(ws.workDir / "main.go", program)
      shell("go", "build", "-o", bin, "main.go")(shell.ShellOption.Cwd(ws.workDir)).bimap(
        e => e.withMessage("go build failed"),
        _ => ()
      )
    }

    val upEvents = EventRecorder()

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
            runtime = ProjectRuntimeInfo(name = "go", options = Map("binary" -> binName))
          )
        ),
        LocalWorkspaceOption.PulumiHome(pulumiHomeDir),
        LocalWorkspaceOption.EnvVars(shell.pulumi.env.PulumiConfigPassphraseEnv -> "test")
      )
      upRes <- upEvents.timing(stack.up(UpOption.OnEvent(upEvents.record)))
      _     <- stack.destroy()
    yield upRes

    res.fold(
      e => fail(e.getMessage, e),
      upRes => {
        assertEquals(upRes.summary.result, Some("succeeded"))
        assertEquals(upRes.parseErrors, Nil)
        assert(upEvents.firstArrivedBeforeReturn, "the first engine event did not arrive before up returned")

        val pre = upEvents.preEvents
        assertEquals(pre.map(_.metadata.urn), upRes.resourcePreEvents.map(_.metadata.urn))

        def urnOf(name: String) = pre.map(_.metadata.urn).find(_.endsWith(s"::$name")).getOrElse(fail(s"no pre event for $name"))
        val (parentUrn, childUrn, grandchildUrn) = (urnOf("parent"), urnOf("child"), urnOf("grandchild"))
        val byUrn                                = pre.map(e => e.metadata.urn -> e.metadata).toMap

        // (c) the explicit parent URN is populated - empty for the root stack, set for everything nested under it
        val (roots, nested) = pre.partition(_.metadata.`type` == "pulumi:pulumi:Stack")
        assert(roots.nonEmpty, "no pulumi:pulumi:Stack ResourcePreEvent")
        assertEquals(roots.flatMap(e => parentOf(e.metadata)).distinct, List(""))
        val rootUrn = roots.head.metadata.urn

        assertEquals(nested.size, 3, s"expected the three components, got: ${nested.map(_.metadata.urn)}")
        assert(
          nested.forall(e => parentOf(e.metadata).exists(_.nonEmpty)),
          s"Expected every nested resource to carry a parent, got: ${nested.map(e => e.metadata.urn -> parentOf(e.metadata))}"
        )
        // and the parent URNs reconstruct exactly the tree the program declared
        assertEquals(parentOf(byUrn(parentUrn)), Some(rootUrn))
        assertEquals(parentOf(byUrn(childUrn)), Some(parentUrn))
        assertEquals(parentOf(byUrn(grandchildUrn)), Some(childUrn))

        // (d) ordering - a parent's ResourcePreEvent always precedes its children's, so a renderer needs no orphan buffering
        val order = pre.map(_.metadata.urn).zipWithIndex.toMap
        val orphans = pre.filter { e =>
          parentOf(e.metadata).filter(_.nonEmpty).exists(parent => !order.get(parent).exists(_ < order(e.metadata.urn)))
        }
        assertEquals(orphans.map(_.metadata.urn), Nil, "a child ResourcePreEvent preceded its parent's")
      }
    )
  }

  FunFixture[FullyQualifiedStackName](
    setup = t => fqsn(this.getClass, t),
    teardown = _ => ()
  ).test("a failed up reports OperationFailedError carrying the engine events") { generatedStackName =>
    val stackName     = FullyQualifiedStackName("goproj", generatedStackName.stack)
    val pulumiHomeDir = os.temp.dir() / ".pulumi"
    loginLocal(pulumiHomeDir)

    // registers one component, then fails the program - enough to produce pre-events and a diagnostic before the non-zero exit
    val program =
      """|package main
         |
         |import (
         |	"fmt"
         |
         |	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
         |)
         |
         |type Group struct{ pulumi.ResourceState }
         |
         |func main() {
         |	pulumi.Run(func(ctx *pulumi.Context) error {
         |		group := &Group{}
         |		if err := ctx.RegisterComponentResource("besom:test:Group", "doomed", group); err != nil {
         |			return err
         |		}
         |		return fmt.Errorf("deliberate failure from the test program")
         |	})
         |}
         |""".stripMargin

    val binName = "failingBinary"
    val binaryBuilder = (ws: Workspace) => {
      os.write.over(ws.workDir / "main.go", program)
      shell("go", "build", "-o", binName, "main.go")(shell.ShellOption.Cwd(ws.workDir)).bimap(
        e => e.withMessage("go build failed"),
        _ => ()
      )
    }

    val upEvents = EventRecorder()

    val res = for
      stack <- createStackRemoteSource(
        stackName,
        GitRepo(url = "https://github.com/pulumi/test-repo.git", projectPath = "goproj", setup = binaryBuilder),
        LocalWorkspaceOption.Project(
          Project(name = "goproj", runtime = ProjectRuntimeInfo(name = "go", options = Map("binary" -> binName)))
        ),
        LocalWorkspaceOption.PulumiHome(pulumiHomeDir),
        LocalWorkspaceOption.EnvVars(shell.pulumi.env.PulumiConfigPassphraseEnv -> "test")
      )
      upRes <- upEvents.timing(stack.up(UpOption.OnEvent(upEvents.record), UpOption.Color(Color.Never)))
    yield upRes

    res match
      case Right(up) => fail(s"expected the up to fail, got: ${up.summary.result}")
      case Left(e: OperationFailedError) =>
        assertEquals(e.operation, "up")
        assertNotEquals(e.exitCode, 0)
        assert(e.getMessage.startsWith("Up failed"), s"unexpected message: ${e.getMessage}")

        // the bulky dump stays on the ShellAutoError cause
        assert(e.cause.exists(_.isInstanceOf[ShellAutoError]), s"expected a ShellAutoError cause, got: ${e.cause}")

        // ── the point of the type: the log is parsed on the failure path, so this data is not lost ──
        assert(e.resourcePreEvents.nonEmpty, "expected resourcePreEvents to survive the failure")
        assert(
          e.resourcePreEvents.exists(_.metadata.urn.endsWith("::doomed")),
          s"expected the component's pre-event, got: ${e.resourcePreEvents.map(_.metadata.urn)}"
        )
        assert(e.diagnostics.nonEmpty, "expected diagnostics to survive the failure")
        assert(
          e.diagnostics.exists(_.message.contains("deliberate failure from the test program")),
          s"expected the program's error among the diagnostics, got: ${e.diagnostics.map(_.message)}"
        )
        assertEquals(e.parseErrors, Nil)
        assert(e.stdout.nonEmpty || e.stderr.nonEmpty, "expected the process output to be carried")

        // live delivery keeps working on the failure path too
        assert(upEvents.preEvents.nonEmpty, "no engine events were delivered live before the failure")
        assert(upEvents.firstArrivedBeforeReturn, "the first engine event did not arrive before up returned")
        assertEquals(upEvents.preEvents.map(_.metadata.urn), e.resourcePreEvents.map(_.metadata.urn))

      case Left(other) => fail(s"expected an OperationFailedError, got ${other.getClass.getName}: ${other.getMessage.take(300)}")
  }

  FunFixture[FullyQualifiedStackName](
    setup = t => fqsn(this.getClass, t),
    teardown = _ => ()
  ).test("setAllConfig plain values round-trip") { generatedStackName =>
    val stackName     = FullyQualifiedStackName("configtest", generatedStackName.stack)
    val pulumiHomeDir = os.temp.dir() / ".pulumi"
    val workDir       = os.temp.dir()
    loginLocal(pulumiHomeDir)

    val res = for
      stack <- createStackLocalSource(
        stackName,
        workDir,
        LocalWorkspaceOption.Project(
          Project(name = "configtest", runtime = "nodejs")
        ),
        LocalWorkspaceOption.PulumiHome(pulumiHomeDir),
        LocalWorkspaceOption.EnvVars(shell.pulumi.env.PulumiConfigPassphraseEnv -> "test")
      )
      _ <- stack.setAllConfig(
        Map(
          "configtest:greeting" -> ConfigValue("hello"),
          "configtest:count" -> ConfigValue("42"),
          "configtest:password" -> ConfigValue("s3cret", secret = true)
        )
      )
      config <- stack.getAllConfig
    yield (stack, config)

    res.fold(
      e => fail(e.getMessage, e),
      (_, config) => {
        assertEquals(config("configtest:greeting").value, "hello")
        assertEquals(config("configtest:greeting").secret, false)
        assertEquals(config("configtest:count").value, "42")
        assertEquals(config("configtest:password").value, "s3cret")
        assertEquals(config("configtest:password").secret, true)
      }
    )
  }

  FunFixture[FullyQualifiedStackName](
    setup = t => fqsn(this.getClass, t),
    teardown = _ => ()
  ).test("setAllConfig with ConfigOption.Json supports secrets") { generatedStackName =>
    val versionStr = os.proc("pulumi", "version").call().out.text().trim.stripPrefix("v")
    val parts      = versionStr.split('.').map(_.takeWhile(_.isDigit).toInt)
    assume(
      parts(0) > 3 || (parts(0) == 3 && parts(1) >= 202),
      s"Pulumi >= 3.202.0 required for --json flag, got $versionStr"
    )

    val stackName     = FullyQualifiedStackName("configtest", generatedStackName.stack)
    val pulumiHomeDir = os.temp.dir() / ".pulumi"
    val workDir       = os.temp.dir()
    loginLocal(pulumiHomeDir)

    val res = for
      stack <- createStackLocalSource(
        stackName,
        workDir,
        LocalWorkspaceOption.Project(
          Project(name = "configtest", runtime = "nodejs")
        ),
        LocalWorkspaceOption.PulumiHome(pulumiHomeDir),
        LocalWorkspaceOption.EnvVars(shell.pulumi.env.PulumiConfigPassphraseEnv -> "test")
      )
      _ <- stack.setAllConfig(
        Map(
          "configtest:greeting" -> ConfigValue("hello"),
          "configtest:password" -> ConfigValue("s3cret", secret = true),
          "configtest:data" -> ConfigValue("""{"key":"value"}""")
        ),
        ConfigOption.Json
      )
      config <- stack.getAllConfig
    yield (stack, config)

    res.fold(
      e => fail(e.getMessage, e),
      (_, config) => {
        // Plain value round-trips
        assertEquals(config("configtest:greeting").value, "hello")
        assertEquals(config("configtest:greeting").secret, false)

        // Secret value round-trips with secret flag
        assertEquals(config("configtest:password").value, "s3cret")
        assertEquals(config("configtest:password").secret, true)

        // JSON string value round-trips as a string
        assertEquals(config("configtest:data").value, """{"key":"value"}""")
        assertEquals(config("configtest:data").secret, false)
      }
    )
  }

end LocalWorkspaceTest
