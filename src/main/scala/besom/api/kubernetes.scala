package besom.api

import besom.util.*
import besom.internal.*
import OutputLift.{*, given}

object kubernetes:

  case class DeploymentMetadata[F[+_]](name: Output[F, NonEmptyString])
  case class Deployment[F[+_]](
    urn: Output[F, NonEmptyString],
    id: Output[F, NonEmptyString],
    metadata: Output[F, DeploymentMetadata[F]]
  )

  def deployment(using
    ctx: Context
  )(
    name: String,
    args: DeploymentArgs[ctx.F],
    opts: ResourceOptions[ctx.F] = CustomResourceOptions(using ctx)()
  ): Output[ctx.F, Deployment[ctx.F]] =
    ???

  case class DeploymentArgs[F[+_]](spec: DeploymentSpecArgs[F])
  object DeploymentArgs:
    def apply(using ctx: Context)(spec: DeploymentSpecArgs[ctx.F]): DeploymentArgs[ctx.F] =
      new DeploymentArgs[ctx.F](spec)

  case class LabelSelectorArgs[F[+_]](
    matchLabels: Output[F, Map[String, String]]
  )
  object LabelSelectorArgs:
    def apply(using ctx: Context)(
      matchLabels: Map[String, String] | Map[String, Output[ctx.F, String]] | Output[ctx.F, Map[String, String]] |
        NotProvided
    ): LabelSelectorArgs[ctx.F] = new LabelSelectorArgs(matchLabels.asOutputMap)

  case class DeploymentSpecArgs[F[+_]](
    selector: LabelSelectorArgs[F],
    replicas: Output[F, Int],
    template: PodTemplateSpecArgs[F]
  )
  object DeploymentSpecArgs:
    def apply(using ctx: Context)(
      selector: LabelSelectorArgs[ctx.F],
      replicas: Int | Output[ctx.F, Int],
      template: PodTemplateSpecArgs[ctx.F]
    ): DeploymentSpecArgs[ctx.F] =
      new DeploymentSpecArgs[ctx.F](selector, replicas.asOutput, template)

  case class PodTemplateSpecArgs[F[+_]](metadata: ObjectMetaArgs[F], spec: PodSpecArgs[F])
  object PodTemplateSpecArgs:
    def apply(using
      ctx: Context
    )(metadata: ObjectMetaArgs[ctx.F], spec: PodSpecArgs[ctx.F]): PodTemplateSpecArgs[ctx.F] =
      new PodTemplateSpecArgs[ctx.F](metadata, spec)

  case class ObjectMetaArgs[F[+_]](
    labels: Output[F, Map[String, String]]
  )
  object ObjectMetaArgs:
    def apply(using ctx: Context)(
      labels: Map[String, String] | Map[String, Output[ctx.F, String]] | Output[ctx.F, Map[String, String]] |
        NotProvided
    ): ObjectMetaArgs[ctx.F] = new ObjectMetaArgs(labels.asOutputMap)

  case class PodSpecArgs[F[+_]](containers: List[ContainerArgs[F]])
  object PodSpecArgs:
    def apply(using ctx: Context)(containers: ContainerArgs[ctx.F]*): PodSpecArgs[ctx.F] =
      new PodSpecArgs(containers.toList)

  case class ContainerArgs[F[+_]](
    name: Output[F, NonEmptyString],
    image: Output[F, NonEmptyString],
    ports: Option[ContainerPortArgs[F]]
  )
  object ContainerArgs:
    def apply(using ctx: Context)(
      name: NonEmptyString | Output[ctx.F, NonEmptyString],
      image: NonEmptyString | Output[ctx.F, NonEmptyString],
      ports: ContainerPortArgs[ctx.F] | NotProvided = NotProvided
    ): ContainerArgs[ctx.F] = new ContainerArgs(name.asOutput, image.asOutput, ports.asOption)

  case class ContainerPortArgs[F[+_]](containerPort: Output[F, Int])
  object ContainerPortArgs:
    def apply(using ctx: Context)(containerPort: Int | Output[ctx.F, Int] | NotProvided): ContainerPortArgs[ctx.F] =
      new ContainerPortArgs(containerPort.asOutput)

  // case class ContainerArgs[F[+_]](
  //   name: Output[F, NonEmptyString],
  //   image: Output[F, NonEmptyString],
  //   ports: Option[ContainerPortArgs[F]]
  // )
  // object ContainerArgs:
  //   def apply[
  //     F[+_],
  //     In1 <: Output[F, NonEmptyString] | F[NonEmptyString] | NonEmptyString,
  //     In2 <: Output[F, NonEmptyString] | F[NonEmptyString] | NonEmptyString
  //   ](using
  //     ctx: Context.Of[F]
  //   )(name: In1, image: In2, ports: ContainerPortArgs[F] | NotProvided): ContainerArgs[ctx.F] =
  //     new ContainerArgs(
  //       OutputLift.lift(name),
  //       OutputLift.lift(image),
  //       ports.asOption
  //     )
