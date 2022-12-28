package besom.api

import besom.util.*
import besom.internal.*
import OutputLift.{*, given}

object kubernetes:
  case class DeploymentArgs[F[+_]]()
  object DeploymentArgs:
    def apply(using ctx: Context)(): DeploymentArgs[ctx.F] = ???

  case class LabelSelectorArgs[F[+_]](
    matchLabels: Output[F, Map[String, String]]
  )
  object LabelSelectorArgs:
    def apply(using ctx: Context)(
      matchLabels: Map[String, String] | Map[String, Output[ctx.F, String]] | Output[ctx.F, Map[String, String]] |
        NotProvided
    ): LabelSelectorArgs[ctx.F] = new LabelSelectorArgs(matchLabels.asOutputMap)

  case class DeploymentSpecArgs[F[+_]]()
  object DeploymentSpecArgs:
    def apply(using ctx: Context)(): DeploymentSpecArgs[ctx.F] = ???

  case class PodTemplateSpecArgs[F[+_]]()
  object PodTemplateSpecArgs:
    def apply(using ctx: Context)(): PodTemplateSpecArgs[ctx.F] = ???

  case class ObjectMetaArgs[F[+_]](
    labels: Output[F, Map[String, String]]
  )
  object ObjectMetaArgs:
    def apply(using ctx: Context)(
      labels: Map[String, String] | Map[String, Output[ctx.F, String]] | Output[ctx.F, Map[String, String]] |
        NotProvided
    ): ObjectMetaArgs[ctx.F] = new ObjectMetaArgs(labels.asOutputMap)

  case class PodSpecArgs[F[+_]](containers: ContainerArgs[F])
  object PodSpecArgs:
    def apply(using ctx: Context)(containers: ContainerArgs[ctx.F]): PodSpecArgs[ctx.F] = new PodSpecArgs(containers)

  // case class ContainerArgs[F[+_]](
  //   name: Output[F, NonEmptyString],
  //   image: Output[F, NonEmptyString],
  //   ports: Option[ContainerPortArgs[F]]
  // )
  // object ContainerArgs:
  //   def apply(using ctx: Context)(
  //     name: NonEmptyString | Output[ctx.F, NonEmptyString],
  //     image: NonEmptyString | Output[ctx.F, NonEmptyString],
  //     ports: ContainerPortArgs[ctx.F] | NotProvided = NotProvided
  //   ): ContainerArgs[ctx.F] = new ContainerArgs(name.asOutput, image.asOutput, ports.asOption)

  case class ContainerPortArgs[F[+_]](containerPort: Output[F, Int])
  object ContainerPortArgs:
    def apply(using ctx: Context)(containerPort: Int | Output[ctx.F, Int] | NotProvided): ContainerPortArgs[ctx.F] =
      new ContainerPortArgs(containerPort.asOutput)

  case class ContainerArgs[F[+_]](
    name: Output[F, NonEmptyString],
    image: Output[F, NonEmptyString],
    ports: Option[ContainerPortArgs[F]]
  )
  object ContainerArgs:
    def apply[
      F[+_],
      In1 <: Output[F, NonEmptyString] | F[NonEmptyString] | NonEmptyString,
      In2 <: Output[F, NonEmptyString] | F[NonEmptyString] | NonEmptyString
    ](using
      ctx: Context.Of[F]
    )(name: In1, image: In2, ports: ContainerPortArgs[F] | NotProvided): ContainerArgs[ctx.F] =
      new ContainerArgs(
        OutputLift.lift(name),
        OutputLift.lift(image),
        ports.asOption
      )
