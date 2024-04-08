package besom.internal

import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.internal.logging.*
import besom.types.*
import besom.util.*
import com.google.protobuf.struct.*

class ResourceDecoderTest extends munit.FunSuite:
  case class CustomStruct(a: String, b: Double) derives Decoder

  case class TestResource(
    urn: Output[URN],
    id: Output[ResourceId],
    property1: Output[Option[String]],
    property2: Output[Int],
    property3: Output[CustomStruct]
  ) extends CustomResource
      derives ResourceDecoder

  val label: Label      = Label.fromNameAndType("test", "test:pkg:TestResource")
  given BesomMDC[Label] = logging.BesomMDC(Key.LabelKey, label)

  def checkOutput[A](
    output: Output[A]
  )(expectedValue: A, expectedDependencies: Set[Resource], expectedIsSecret: Boolean): Unit =
    (output.getData.unsafeRunSync(): @unchecked) match
      case OutputData.Known(dependencies, isSecret, Some(value)) =>
        assertEquals(value, expectedValue)
        assert(clue(dependencies) == clue(expectedDependencies))
        assert(clue(isSecret) == clue(expectedIsSecret)) // TODO: test some output with isSecret = true?
      case obtained => fail(s"Expected OutputData.Known", clues(expectedValue, obtained.getValue))

  // noinspection TypeAnnotation
  def assertDecodingError[A](output: Output[A]) =
    try
      output.getData.unsafeRunSync()
      fail(s"Expected failed output, got successful output: ${output.getData.unsafeRunSync()}")
    catch
      case _: besom.internal.DecodingError           => // OK, we have the error
      case _: besom.internal.AggregatedDecodingError => // OK, we have aggregated errors
      case t: Throwable                              => fail(s"Expected DecodingError", clues(t))

  runWithBothOutputCodecs {
    test(s"resource resolver - happy path (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val resourceDecoder    = summon[ResourceDecoder[TestResource]]
      val dependencyResource = DependencyResource(Output(URN.empty))

      val errorOrResourceResult = Right(
        RawResourceResult(
          urn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"),
          id = Some("fakeId"),
          data = Struct(
            Map(
              "property1" -> Some("abc").asValue,
              "property2" -> 123.asValue,
              "property3" -> Map(
                "a" -> "xyz".asValue,
                "b" -> 3.0d.asValue
              ).asValue
            )
          ),
          dependencies = Map(
            "property1" -> Set.empty,
            "property2" -> Set(dependencyResource)
          )
        )
      )

      val (resource, resourceResolver) = resourceDecoder.makeResourceAndResolver.unsafeRunSync()

      resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

      checkOutput(resource.urn)(
        "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource",
        Set(resource),
        false
      )
      checkOutput(resource.id)("fakeId", Set(resource), false)
      checkOutput(resource.property1)(Some("abc"), Set(resource), false)
      checkOutput(resource.property2)(123, Set(resource, dependencyResource), false)
      checkOutput(resource.property3)(CustomStruct("xyz", 3.0d), Set(resource), false)
    }
  }

  runWithBothOutputCodecs {
    test(s"resource resolver - missing required property (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val resourceDecoder = summon[ResourceDecoder[TestResource]]

      val resourceId: ResourceId = "fakeId"
      val viaUnsafeOf            = ResourceId.unsafeOf(resourceId)
      assert(viaUnsafeOf == resourceId)

      val errorOrResourceResult = Right(
        RawResourceResult(
          urn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"),
          id = Some(resourceId),
          data = Struct(
            Map(
              "property1" -> Some("abc").asValue,
              "property2" -> 123.asValue
              // property3 is missing
            )
          ),
          dependencies = Map(
            "property1" -> Set.empty,
            "property2" -> Set.empty
          )
        )
      )

      val (resource, resourceResolver) = resourceDecoder.makeResourceAndResolver.unsafeRunSync()

      resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

      checkOutput(resource.urn)(
        "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource",
        Set(resource),
        false
      )
      checkOutput(resource.id)("fakeId", Set(resource), false)
      checkOutput(resource.property1)(Some("abc"), Set(resource), false)
      checkOutput(resource.property2)(123, Set(resource), false)
      assertDecodingError(resource.property3)
    }
  }

  runWithBothOutputCodecs {
    test(
      s"resource resolver - required property received as NullValue, issue #150 (keepOutputValues: ${Context().featureSupport.keepOutputValues})"
    ) {
      val resourceDecoder    = summon[ResourceDecoder[TestResource]]
      val dependencyResource = DependencyResource(Output(URN.empty))

      val errorOrResourceResult = Right(
        RawResourceResult(
          urn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"),
          id = Some(ResourceId.unsafeOf("fakeId")),
          data = Struct(
            Map(
              "property1" -> Some("abc").asValue,
              "property2" -> 123.asValue,
              "property3" -> Null
            )
          ),
          dependencies = Map(
            "property1" -> Set.empty,
            "property2" -> Set(dependencyResource)
          )
        )
      )

      val (resource, resourceResolver) = resourceDecoder.makeResourceAndResolver.unsafeRunSync()

      resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

      checkOutput(resource.urn)(
        "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource",
        Set(resource),
        false
      )
      checkOutput(resource.id)("fakeId", Set(resource), false)
      checkOutput(resource.property1)(Some("abc"), Set(resource), false)
      checkOutput(resource.property2)(123, Set(resource, dependencyResource), false)
      assertDecodingError(resource.property3)
    }
  }

  final case class PortResponse(port: Int, protocol: Option[String])
  object PortResponse:
    given decoder(using Context): Decoder[PortResponse] = Decoder.derived[PortResponse]

  final case class IpAddressResponse(
    autoGeneratedDomainNameLabelScope: String, // FIXME: should be optional
    dnsNameLabel: Option[String],
    fqdn: String,
    ip: Option[String],
    ports: List[PortResponse],
    `type`: String
  )
  object IpAddressResponse:
    given decoder(using Context): Decoder[IpAddressResponse] = Decoder.derived[IpAddressResponse]

  final case class ContainerGroup(
    urn: Output[URN],
    id: Output[ResourceId],
    ipAddress: Output[Option[IpAddressResponse]]
  ) extends CustomResource
  object ContainerGroup:
    given resourceDecoder(using Context): ResourceDecoder[ContainerGroup] = ResourceDecoder.derived[ContainerGroup]
    given decoder(using Context): Decoder[ContainerGroup]                 = Decoder.customResourceDecoder[ContainerGroup]

  runWithBothOutputCodecs {
    test(s"partially valid resource") {
      val resourceDecoder = summon[ResourceDecoder[ContainerGroup]]

      val errorOrResourceResult = Right(
        RawResourceResult(
          urn = URN("urn:pulumi:dev::azure-aci-bootzooka::azure-native:containerinstance:ContainerGroup::bootzooka-container-group"),
          id = Some(
            ResourceId.unsafeOf(
              "/subscriptions/eddfb579-a139-4e1a-9843-b05e08a03aa4/resourceGroups/bootzooka-resource-group628b9662/providers/Microsoft.ContainerInstance/containerGroups/bootzooka-container-group6f1306e5"
            )
          ),
          data = Struct(
            fields = Map(
              "ipAddress" -> Value(
                kind = Value.Kind.StructValue(
                  value = Struct(
                    fields = Map(
                      "ip" -> Value(kind = Value.Kind.StringValue(value = "4.157.146.145")),
                      "ports" -> Value(
                        kind = Value.Kind.ListValue(
                          value = ListValue(
                            values = Vector(
                              Value(
                                kind = Value.Kind.StructValue(
                                  value = Struct(
                                    fields = Map(
                                      "port" -> Value(kind = Value.Kind.NumberValue(value = 8080.0)),
                                      "protocol" -> Value(kind = Value.Kind.StringValue(value = "TCP"))
                                    )
                                  )
                                )
                              )
                            )
                          )
                        )
                      ),
                      "type" -> Value(kind = Value.Kind.StringValue(value = "Public")),
                      /* TODO: handle missing fields */
                      "fqdn" -> Value(kind =
                        Value.Kind.StringValue(value = "bootzooka-container-group6f1306e5.westeurope.azurecontainer.io")
                      ),
                      "autoGeneratedDomainNameLabelScope" -> Value(kind =
                        Value.Kind.StringValue(value = "bootzooka-container-group6f1306e5")
                      )
                    )
                  )
                )
              )
            )
          ),
          dependencies = Map.empty
        )
      )

      val (resource, resourceResolver) = resourceDecoder.makeResourceAndResolver.unsafeRunSync()

      resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

      checkOutput(resource.urn)(
        "urn:pulumi:dev::azure-aci-bootzooka::azure-native:containerinstance:ContainerGroup::bootzooka-container-group",
        expectedDependencies = Set(resource),
        expectedIsSecret = false
      )

      checkOutput(resource.id)(
        "/subscriptions/eddfb579-a139-4e1a-9843-b05e08a03aa4/resourceGroups/bootzooka-resource-group628b9662/providers/Microsoft.ContainerInstance/containerGroups/bootzooka-container-group6f1306e5",
        expectedDependencies = Set(resource),
        expectedIsSecret = false
      )

      val ip: Output[Option[String]] = resource.ipAddress.map(_.flatMap(_.ip))
      checkOutput(ip)(
        Some("4.157.146.145"),
        expectedDependencies = Set(resource),
        expectedIsSecret = false
      )

      val ports: Output[List[PortResponse]] = resource.ipAddress.map(_.map(_.ports).getOrElse(List.empty))
      checkOutput(ports)(
        List(PortResponse(port = 8080, protocol = Some("TCP"))),
        expectedDependencies = Set(resource),
        expectedIsSecret = false
      )

      // TODO: handle missing fields
//      assertDecodingError (resource.ipAddress.map(_.map(_.autoGeneratedDomainNameLabelScope))
//      assertDecodingError (resource.ipAddress.map(_.map(_.fqdn))
    }
  }
end ResourceDecoderTest
