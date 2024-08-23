package besom.internal

import besom.json.*
import besom.types.*
import besom.util.NonEmptyString

case class StackReference(
  urn: Output[URN],
  id: Output[ResourceId],
  name: Output[String],
  outputs: Output[Map[String, JsValue]],
  secretOutputNames: Output[Set[String]]
) extends CustomResource
    derives ResourceDecoder:

  def getOutput(name: NonEmptyString)(using Context): Output[Option[JsValue]] =
    getOutput(Output(name))

  def getOutput(name: Output[NonEmptyString]): Output[Option[JsValue]] =
    val output = name.zip(outputs).map { case (name, outputs) =>
      outputs.get(name)
    }

    output.withIsSecret(isSecretOutputName(name))

  def requireOutput(name: NonEmptyString)(using Context): Output[JsValue] =
    requireOutput(Output(name))

  def requireOutput(name: Output[NonEmptyString])(using Context): Output[JsValue] =
    val output = name.zip(outputs).flatMap { case (name, outputs) =>
      outputs.get(name) match
        case Some(value) => Output(value)
        case None        => Output.fail(Exception(s"Missing required output '$name'"))
    }

    output.withIsSecret(isSecretOutputName(name))

  private def isSecretOutputName(name: Output[String]): Result[Boolean] =
    for
      nameOd  <- name.getData
      namesOd <- secretOutputNames.getData
      isSecret <- {
        // If either the name or set of secret outputs is unknown, we can't do anything smart,
        // so we just copy the secret-ness from the entire outputs value.
        if !(nameOd.known && namesOd.known) then outputs.getData.map(_.secret)
        // Otherwise, if we have a set of outputs we know are secret,
        // we can use it to determine if this output should be secret.
        else Result.pure(namesOd.getValueOrElse(Set.empty).contains(nameOd.getValueOrElse("")))
      }
    yield isSecret
end StackReference

trait StackReferenceFactory:
  sealed trait StackReferenceType[T]:
    type Out[T]
    def transform(stackReference: StackReference): Output[Out[T]]

  class TypedStackReferenceType[T](using JsonReader[T]) extends StackReferenceType[T]:
    type Out[T] = TypedStackReference[T]
    def transform(stackReference: StackReference): Output[Out[T]] =
      val objectOutput: Output[T] =
        requireObject(stackReference.outputs, stackReference.secretOutputNames)

      objectOutput.map(t =>
        TypedStackReference(
          urn = stackReference.urn,
          id = stackReference.id,
          name = stackReference.name,
          outputs = t,
          secretOutputNames = stackReference.secretOutputNames
        )
      )

  class UntypedStackReferenceType(using Context) extends StackReferenceType[Any]:
    type Out[T] = StackReference
    def transform(stackReference: StackReference): Output[StackReference] = Output(stackReference)

  import scala.compiletime.summonFrom
  inline implicit def stackRefTypeProvider[T](using Context): StackReferenceType[T] =
    summonFrom {
      case _: besom.json.JsonReader[T] => typedStackReference[T]
      case _                           => untypedStackReference.asInstanceOf[StackReferenceType[T]]
    }

  def untypedStackReference(using Context): StackReferenceType[Any] = UntypedStackReferenceType()

  def typedStackReference[T: JsonReader]: TypedStackReferenceType[T] = TypedStackReferenceType()

  def apply[T](using stackRefType: StackReferenceType[T], ctx: Context)(
    name: NonEmptyString,
    args: Input.Optional[StackReferenceArgs] = None,
    opts: StackReferenceResourceOptions = StackReferenceResourceOptions()
  ): Output[stackRefType.Out[T]] =
    args
      .asOptionOutput(false)
      .flatMap {
        case Some(stackRefArgs) => stackRefArgs.name
        case None               => Output(name)
      }
      .flatMap { selectedName =>
        val importId = ResourceId.unsafeOf(selectedName)

        val stackRefArgs = StackReferenceArgs(
          Output(selectedName)
        )

        val mergedOpts = new StackReferenceResourceOptions( // use constructor directly to avoid apply
          opts.common,
          Output(Some(importId))
        )

        Context().readOrRegisterResource[StackReference, StackReferenceArgs]("pulumi:pulumi:StackReference", name, stackRefArgs, mergedOpts)
      }
      .flatMap(stackRefType.transform)

  // def apply[T](using
  //   ctx: Context,
  //   jr: JsonReader[T]
  // )(name: NonEmptyString, args: Input.Optional[StackReferenceArgs], opts: StackReferenceResourceOptions): Output[TypedStackReference[T]] =
  //   apply(using ctx)(name, args, opts).flatMap { stackReference =>
  //     val objectOutput: Output[T] =
  //       requireObject(stackReference.outputs, stackReference.secretOutputNames)

  //     objectOutput.map(t =>
  //       TypedStackReference(
  //         urn = stackReference.urn,
  //         id = stackReference.id,
  //         name = stackReference.name,
  //         outputs = t,
  //         secretOutputNames = stackReference.secretOutputNames
  //       )
  //     )
  //   }

  private[internal] def requireObject[T: JsonReader](
    outputs: Output[Map[String, JsValue]],
    secretOutputNames: Output[Set[String]]
  ): Output[T] =
    outputs
      .map(JsObject(_).convertTo[T])
      .withIsSecret(
        secretOutputNames
          .map(_.nonEmpty)
          .getValueOrElse(false)
      )
end StackReferenceFactory
