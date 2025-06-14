package besom.internal

import besom.types.{URN, ResourceId, ResourceType}
import RunOutput.{*, given}
import ProtobufUtil.given

import pulumirpc.resource.{RegisterResourceRequest, RegisterResourceResponse, RegisterResourceOutputsRequest}
import com.google.protobuf.struct.Struct
import com.google.protobuf.struct.*
import besom.util.NonEmptyString

class ContextPropagationTest extends munit.FunSuite:

  case class TestResource(urn: Output[URN], id: Output[ResourceId], url: Output[String]) extends CustomResource derives ResourceDecoder
  object TestResource extends ResourceCompanion[TestResource, EmptyArgs]:
    val typeToken: ResourceType = ResourceType.unsafeOf("test:resource:TestResource")
    def apply(name: NonEmptyString): Output[TestResource] = Output.getContext.flatMap { implicit ctx =>
      ctx.readOrRegisterResource[TestResource, EmptyArgs](
        typeToken,
        name,
        EmptyArgs(),
        CustomResourceOptions()
      )
    }

  case class TestComponentResource(url: Output[String])(using ComponentBase) extends ComponentResource
  object TestComponentResource extends ResourceCompanion[TestComponentResource, EmptyArgs]:
    val typeToken: ResourceType = ResourceType.unsafeOf("test:component:TestComponentResource")

  test("context propagation - plain resource, no parent") {
    val stackUrn = URN(
      "urn:pulumi:stack::project::stack:Stack$besom:testing/test:Stack::test-stack"
    )
    val resourceUrn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::test-resource")
    val spyMonitor = new DummyContext.DummyMonitor:
      override def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse] = Result.defer {
        assert(registerResourceRequest.parent == stackUrn.asString)

        val obj: Struct = Map("url" -> "https://test.com".asValue).asStruct

        RegisterResourceResponse(urn = resourceUrn.asString, `object` = Some(obj), id = "test-id")
      }

    val ctx: Context = DummyContext(monitor = spyMonitor, stackURN = stackUrn).unsafeRunSync()

    val resource = TestResource("test-resource").unsafeRunSync()(using ctx)

    resource match
      case None => fail("Expected resource to be defined")
      case Some(res) =>
        res.urn.getValue(using ctx).unsafeRunSync() match
          case Some(urn) =>
            assert(urn == resourceUrn)
          case None => fail("Expected resource urn to be defined")

        res.id.getValue(using ctx).unsafeRunSync() match
          case Some(id) =>
            assert(id == ResourceId.unsafeOf("test-id"))
          case None => fail("Expected resource id to be defined")

        res.url.getValue(using ctx).unsafeRunSync() match
          case Some(url) =>
            assert(url == "https://test.com")
          case None => fail("Expected resource url to be defined")
  }

  test("context propagation - resource in component") {
    val stackUrn = URN(
      "urn:pulumi:stack::project::stack:Stack$besom:testing/test:Stack::test-stack"
    )
    val componentUrn = URN("urn:pulumi:stack::project::custom:components:Component$besom:testing/test:Component::test-component")

    val resourceUrn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::test-resource")

    val spyMonitor = new DummyContext.DummyMonitor:
      override def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse] = Result.defer {
        registerResourceRequest.`type` -> registerResourceRequest.name match
          case (TestComponentResource.typeToken, "test-component") =>
            assert(registerResourceRequest.parent == stackUrn.asString)

            val obj: Struct = Map.empty[String, Value].asStruct

            RegisterResourceResponse(urn = componentUrn.asString, `object` = Some(obj))
          case (TestResource.typeToken, "test-resource") =>
            assert(registerResourceRequest.parent == componentUrn.asString)

            val obj: Struct = Map("url" -> "https://test.com".asValue).asStruct

            RegisterResourceResponse(urn = resourceUrn.asString, `object` = Some(obj), id = "test-id")

          case _ =>
            fail("Unexpected resource type")
      }

      override def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): Result[Unit] =
        Result.pure(())

    val ctx: Context = DummyContext(monitor = spyMonitor, stackURN = stackUrn).unsafeRunSync()

    val comp = besom
      .component("test-component", TestComponentResource.typeToken, ComponentResourceOptions()) {
        val nested = TestResource("test-resource")

        TestComponentResource(nested.flatMap(_.url))
      }
      .unsafeRunSync()(using ctx)

    comp match
      case None => fail("Expected component to be defined")
      case Some(compRes) =>
        compRes.urn.getValue(using ctx).unsafeRunSync() match
          case Some(urn) =>
            assert(urn == componentUrn)
            compRes.url.getValue(using ctx).unsafeRunSync() match
              case Some(url) =>
                assert(url == "https://test.com")
              case None => fail("Expected component url to be defined")
          case None => fail("Expected component urn to be defined")
  }

end ContextPropagationTest
