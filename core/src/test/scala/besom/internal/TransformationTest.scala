package besom.internal

import besom.types.{URN, ResourceId, ResourceType}
import RunOutput.{*, given}
import ProtobufUtil.given

import pulumirpc.resource.{RegisterResourceRequest, RegisterResourceResponse}
import com.google.protobuf.struct.*
import besom.util.NonEmptyString

class TransformationTest extends munit.FunSuite:
  case class SimpleArgs(testValue: Output[String]) derives Encoder, ArgsEncoder

  case class Simple(
    urn: Output[URN],
    id: Output[ResourceId]
  ) extends CustomResource
      derives ResourceDecoder

  object Simple extends ResourceCompanion[Simple, SimpleArgs]:
    val typeToken: ResourceType = ResourceType.unsafeOf("test:resource:Simple")
    def apply(name: NonEmptyString, args: SimpleArgs, opts: CustomResourceOptions)(using ctx: Context) =
      ctx.readOrRegisterResource[Simple, SimpleArgs](
        typeToken,
        name,
        args,
        opts
      )

  case class Component(
    testValue: Output[String]
  )(using ComponentBase)
      extends ComponentResource

  object Component extends ResourceCompanion[Component, EmptyArgs]:
    val typeToken: ResourceType = ResourceType.unsafeOf("test:component:Component")
    def apply(name: NonEmptyString, opts: ComponentResourceOptions)(using ctx: Context) =
      ctx.registerComponentResource(
        name,
        typeToken,
        opts
      )

  case class NestedArgs(innerValue: Output[SimpleArgs]) derives ArgsEncoder

  case class Nested(
    urn: Output[URN],
    id: Output[ResourceId]
  ) extends CustomResource
      derives ResourceDecoder

  object Nested extends ResourceCompanion[Nested, NestedArgs]:
    val typeToken: ResourceType = ResourceType.unsafeOf("test:resource:Nested")
    def apply(name: NonEmptyString, args: NestedArgs, opts: CustomResourceOptions)(using ctx: Context) =
      ctx.readOrRegisterResource[Nested, NestedArgs](
        typeToken,
        name,
        args,
        opts
      )

  def captureRegisteredArgs(f: Context ?=> Unit): Map[String, Option[Struct]] = {
    val fakeUrn        = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::test-resource")
    val registeredArgs = scala.collection.mutable.Map.empty[String, Option[Struct]]

    val spyMonitor = new DummyContext.DummyMonitor:
      override def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse] = Result.pure {
        registeredArgs(registerResourceRequest.name) = registerResourceRequest.`object`
        RegisterResourceResponse(urn = fakeUrn.asString, `object` = Some(Map.empty[String, Value].asStruct))
      }

    given ctx: Context = DummyContext(monitor = spyMonitor).unsafeRunSync()
    f(using ctx)
    ctx.waitForAllTasks.unsafeRunSync()
    registeredArgs.toMap
  }

  test("transform args of resource directly") {
    val transformation = Simple.transformArgs { testArgs =>
      SimpleArgs(testValue = testArgs.testValue.map(v => s"transformed-$v"))
    }

    val registeredArgs = captureRegisteredArgs {
      Simple("simple", SimpleArgs(testValue = Output.pure("abc")), CustomResourceOptions(transformations = List(transformation)))
        .unsafeRunSync()
    }

    val transformedValue = registeredArgs("simple").get.fields("testValue").getStructValue.fields("value").getStringValue
    assertEquals(transformedValue, "transformed-abc")
  }

  test("transform args of child resource") {
    val transformation = Simple.transformArgs { testArgs =>
      SimpleArgs(testValue = testArgs.testValue.map(v => s"transformed-$v"))
    }

    val registeredArgs = captureRegisteredArgs {
      val component = Component("component", ComponentResourceOptions(transformations = List(transformation))).unsafeRunSync()
      Simple("simple", SimpleArgs(testValue = Output.pure("abc")), CustomResourceOptions(parent = Some(component))).unsafeRunSync()
    }

    val transformedValue = registeredArgs("simple").get.fields("testValue").getStructValue.fields("value").getStringValue
    assertEquals(transformedValue, "transformed-abc")
  }

  test("transform args of resource in correct order") {
    val transformation1 = Simple.transformArgs { testArgs =>
      SimpleArgs(testValue = testArgs.testValue.map(s => s"${s}-v1"))
    }
    val transformation2 = Simple.transformArgs { testArgs =>
      SimpleArgs(testValue = testArgs.testValue.map(s => s"${s}-v2"))
    }
    val transformation3 = Simple.transformArgs { testArgs =>
      SimpleArgs(testValue = testArgs.testValue.map(s => s"${s}-v3"))
    }

    val registeredArgs = captureRegisteredArgs {
      val component = Component(
        "component",
        ComponentResourceOptions(
          transformations = List(transformation1)
        )
      ).unsafeRunSync()

      Simple(
        "simple",
        SimpleArgs(testValue = Output.pure("abc")),
        CustomResourceOptions(
          parent = Some(component),
          transformations = List(transformation2, transformation3)
        )
      ).unsafeRunSync()
    }

    // Parent transformations applied at the end
    val transformedValue = registeredArgs("simple").get.fields("testValue").getStructValue.fields("value").getStringValue
    assertEquals(transformedValue, "abc-v2-v3-v1")
  }

  test("transform args of resource with filter") {
    val transformation = Simple.when(_.name.endsWith("-1")).transformArgs { testArgs =>
      SimpleArgs(testValue = testArgs.testValue.map(v => s"transformed-$v"))
    }

    val registeredArgs = captureRegisteredArgs {
      Simple("simple-1", SimpleArgs(testValue = Output.pure("abc")), CustomResourceOptions(transformations = List(transformation)))
        .unsafeRunSync()
      Simple("simple-2", SimpleArgs(testValue = Output.pure("xyz")), CustomResourceOptions(transformations = List(transformation)))
        .unsafeRunSync()
    }

    val transformedValue1 = registeredArgs("simple-1").get.fields("testValue").getStructValue.fields("value").getStringValue
    val transformedValue2 = registeredArgs("simple-2").get.fields("testValue").getStructValue.fields("value").getStringValue
    assertEquals(transformedValue1, "transformed-abc")
    assertEquals(transformedValue2, "xyz")
  }

  test("transform nested args of resource") {
    val transformation = Nested.transformNestedArgs[SimpleArgs] { simpleArgs =>
      SimpleArgs(testValue = simpleArgs.testValue.map(v => s"transformed-$v"))
    }

    val registeredArgs = captureRegisteredArgs {
      Nested(
        "simple",
        NestedArgs(innerValue = Output.pure(SimpleArgs(testValue = Output.pure("abc")))),
        CustomResourceOptions(transformations = List(transformation))
      ).unsafeRunSync()
    }

    val transformedValue = registeredArgs("simple").get
      .fields("innerValue")
      .getStructValue
      .fields("value")
      .getStructValue
      .fields("testValue")
      .getStructValue
      .fields("value")
      .getStringValue
    assertEquals(transformedValue, "transformed-abc")
  }
end TransformationTest
