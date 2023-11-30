package besom.internal

import com.google.protobuf.struct.*, Value.Kind
import ProtobufUtil.*
import RunResult.{given, *}

object ArgsEncoderTest:

  final case class ProviderArgs private (
    cluster: besom.types.Output[scala.Option[String]],
    context: besom.types.Output[scala.Option[String]],
    deleteUnreachable: besom.types.Output[scala.Option[Boolean]],
    enableConfigMapMutable: besom.types.Output[scala.Option[Boolean]],
    enableServerSideApply: besom.types.Output[scala.Option[Boolean]],
    kubeconfig: besom.types.Output[scala.Option[String]],
    namespace: besom.types.Output[scala.Option[String]],
    renderYamlToDirectory: besom.types.Output[scala.Option[String]],
    skipUpdateUnreachable: besom.types.Output[scala.Option[Boolean]],
    suppressDeprecationWarnings: besom.types.Output[scala.Option[Boolean]],
    suppressHelmHookWarnings: besom.types.Output[scala.Option[Boolean]]
  ) derives besom.types.ProviderArgsEncoder

  object ProviderArgs:
    def apply(
      cluster: besom.types.Input.Optional[String] = None,
      context: besom.types.Input.Optional[String] = None,
      deleteUnreachable: besom.types.Input.Optional[Boolean] = None,
      enableConfigMapMutable: besom.types.Input.Optional[Boolean] = None,
      enableServerSideApply: besom.types.Input.Optional[Boolean] = None,
      kubeconfig: besom.types.Input.Optional[String] = None,
      namespace: besom.types.Input.Optional[String] = None,
      renderYamlToDirectory: besom.types.Input.Optional[String] = None,
      skipUpdateUnreachable: besom.types.Input.Optional[Boolean] = None,
      suppressDeprecationWarnings: besom.types.Input.Optional[Boolean] = None,
      suppressHelmHookWarnings: besom.types.Input.Optional[Boolean] = None
    )(using besom.types.Context): ProviderArgs =
      new ProviderArgs(
        cluster = cluster.asOptionOutput(isSecret = false),
        context = context.asOptionOutput(isSecret = false),
        deleteUnreachable = deleteUnreachable.asOptionOutput(isSecret = false),
        enableConfigMapMutable = enableConfigMapMutable.asOptionOutput(isSecret = false),
        enableServerSideApply = enableServerSideApply.asOptionOutput(isSecret = false),
        kubeconfig = kubeconfig.asOptionOutput(isSecret = false),
        namespace = namespace.asOptionOutput(isSecret = false),
        renderYamlToDirectory = renderYamlToDirectory.asOptionOutput(isSecret = false),
        skipUpdateUnreachable = skipUpdateUnreachable.asOptionOutput(isSecret = false),
        suppressDeprecationWarnings = suppressDeprecationWarnings.asOptionOutput(isSecret = false),
        suppressHelmHookWarnings = suppressHelmHookWarnings.asOptionOutput(isSecret = false)
      )
end ArgsEncoderTest

class ArgsEncoderTest extends munit.FunSuite with ValueAssertions:
  import ArgsEncoderTest.*

  test("encode ProviderArgs") {
    val pae       = summon[ProviderArgsEncoder[ProviderArgs]]
    given Context = DummyContext().unsafeRunSync()

    val expected = Map(
      "kubeconfig" -> "abcd".asValue,
    ).asValue

    val (_, encoded) = pae.encode(ProviderArgs(kubeconfig = "abcd"), _ => false).unsafeRunSync()

    assertEqualsValue(Value(Kind.StructValue(encoded)), expected)
  }
