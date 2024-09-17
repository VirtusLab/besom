package besom.internal

import besom.json.*
import besom.util.NonEmptyString
import besom.types.{URN, ResourceId}

case class StackReference(
  urn: Output[URN],
  id: Output[ResourceId],
  name: Output[String],
  outputs: Output[Map[String, JsValue]],
  secretOutputNames: Output[Set[String]]
) extends CustomResource
    derives ResourceDecoder:

  def getOutput(name: NonEmptyString): Output[Option[JsValue]] =
    getOutput(Output.pure(name))

  def getOutput(name: Output[NonEmptyString]): Output[Option[JsValue]] =
    val output = name.zip(outputs).map { case (name, outputs) =>
      outputs.get(name)
    }

    Output.getContext.flatMap { ctx =>
      output.withIsSecret(isSecretOutputName(name)(using ctx))
    }

  def requireOutput(name: NonEmptyString): Output[JsValue] =
    requireOutput(Output.pure(name))

  def requireOutput(name: Output[NonEmptyString]): Output[JsValue] =
    val output = name.zip(outputs).flatMap { case (name, outputs) =>
      outputs.get(name) match
        case Some(value) => Output.pure(value)
        case None        => Output.fail(Exception(s"Missing required output '$name'"))
    }

    Output.getContext.flatMap { ctx =>
      output.withIsSecret(isSecretOutputName(name)(using ctx))
    }

  private def isSecretOutputName(name: Output[String])(using Context): Result[Boolean] =
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

  object StackReferenceType:
    given untyped: UntypedStackReferenceType = UntypedStackReferenceType()

    given typed[T: JsonReader]: TypedStackReferenceType[T] = TypedStackReferenceType[T]

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

  class UntypedStackReferenceType extends StackReferenceType[Any]:
    type Out[T] = StackReference
    def transform(stackReference: StackReference): Output[StackReference] = Output.pure(stackReference)

  def untypedStackReference: StackReferenceType[Any] = UntypedStackReferenceType()

  def typedStackReference[T: JsonReader]: TypedStackReferenceType[T] = TypedStackReferenceType()

  def apply[T](
    name: NonEmptyString,
    args: Input.Optional[StackReferenceArgs] = None,
    opts: StackReferenceResourceOptions = StackReferenceResourceOptions()
  )(using stackRefType: StackReferenceType[T]): Output[stackRefType.Out[T]] =
    args
      .asOptionOutput(false)
      .flatMap {
        case Some(stackRefArgs) => stackRefArgs.name
        case None               => Output.pure(name)
      }
      .flatMap { selectedName =>
        val importId = ResourceId.unsafeOf(selectedName)

        val stackRefArgs = StackReferenceArgs(
          Output.pure(selectedName)
        )

        val mergedOpts = new StackReferenceResourceOptions( // use constructor directly to avoid apply
          opts.common,
          Output.pure(Some(importId))
        )

        Output.getContext.flatMap { implicit ctx =>
          ctx.readOrRegisterResource[StackReference, StackReferenceArgs]("pulumi:pulumi:StackReference", name, stackRefArgs, mergedOpts)
        }
      }
      .flatMap(stackRefType.transform)

  private[internal] def requireObject[T: JsonReader](
    outputs: Output[Map[String, JsValue]],
    secretOutputNames: Output[Set[String]]
  ): Output[T] =
    Output.getContext.flatMap { ctx =>
      outputs
        .map(JsObject(_).convertTo[T])
        .withIsSecret(
          secretOutputNames
            .map(_.nonEmpty)
            .getValueOrElse(false)(using ctx)
        )
    }

end StackReferenceFactory
