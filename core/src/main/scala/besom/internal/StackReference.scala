package besom.internal

import spray.json.*
import besom.types.*

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

object StackReference:
  def apply(name: NonEmptyString, args: Input.Optional[StackReferenceArgs], opts: StackReferenceResourceOptions)(using
    Context
  ): Output[StackReference] =
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

        val mergedOpts = StackReferenceResourceOptions(
          opts.common,
          Some(importId)
        )

        Context().readOrRegisterResource[StackReference, StackReferenceArgs]("pulumi:pulumi:StackReference", name, stackRefArgs, mergedOpts)
      }
