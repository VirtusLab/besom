package besom.api

import besom.util.*
import besom.internal.*
// import OutputLift.{*, given}

object kubernetes:

  case class DeploymentMetadata(name: Output[NonEmptyString])
  case class Deployment(
    urn: Output[NonEmptyString],
    id: Output[NonEmptyString],
    metadata: Output[DeploymentMetadata]
  )

  def deployment(using
    ctx: Context
  )(
    name: String,
    args: DeploymentArgs,
    opts: ResourceOptions = CustomResourceOptions(using ctx)()
  ): Output[Deployment] =
    ???

  case class DeploymentArgs(spec: DeploymentSpecArgs)
  object DeploymentArgs:
    def apply(using ctx: Context)(spec: DeploymentSpecArgs): DeploymentArgs =
      new DeploymentArgs(spec)

  case class LabelSelectorArgs(
    matchLabels: Output[Map[String, String]]
  )
  object LabelSelectorArgs:
    def apply(using ctx: Context)(
      matchLabels: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided
    ): LabelSelectorArgs = new LabelSelectorArgs(matchLabels.asOutputMap)

  case class DeploymentSpecArgs(
    selector: LabelSelectorArgs,
    replicas: Output[Int],
    template: PodTemplateSpecArgs
  )
  object DeploymentSpecArgs:
    def apply(using ctx: Context)(
      selector: LabelSelectorArgs,
      replicas: Int | Output[Int],
      template: PodTemplateSpecArgs
    ): DeploymentSpecArgs =
      new DeploymentSpecArgs(selector, replicas.asOutput, template)

  case class PodTemplateSpecArgs(metadata: ObjectMetaArgs, spec: PodSpecArgs)
  object PodTemplateSpecArgs:
    def apply(using
      ctx: Context
    )(metadata: ObjectMetaArgs, spec: PodSpecArgs): PodTemplateSpecArgs =
      new PodTemplateSpecArgs(metadata, spec)

  case class ObjectMetaArgs(
    labels: Output[Map[String, String]]
  )
  object ObjectMetaArgs:
    def apply(using ctx: Context)(
      labels: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided
    ): ObjectMetaArgs = new ObjectMetaArgs(labels.asOutputMap)

  case class PodSpecArgs(containers: List[ContainerArgs])
  object PodSpecArgs:
    def apply(using ctx: Context)(containers: ContainerArgs*): PodSpecArgs =
      new PodSpecArgs(containers.toList)

  case class ContainerArgs(
    name: Output[NonEmptyString],
    image: Output[NonEmptyString],
    ports: Option[ContainerPortArgs]
  )
  object ContainerArgs:
    def apply(using ctx: Context)(
      name: NonEmptyString | Output[NonEmptyString],
      image: NonEmptyString | Output[NonEmptyString],
      ports: ContainerPortArgs | NotProvided = NotProvided
    ): ContainerArgs = new ContainerArgs(name.asOutput, image.asOutput, ports.asOption)

  case class ContainerPortArgs(containerPort: Output[Int])
  object ContainerPortArgs:
    def apply(using ctx: Context)(containerPort: Int | Output[Int] | NotProvided): ContainerPortArgs =
      new ContainerPortArgs(containerPort.asOutput)

  // case class ContainerArgs(
  //   name: Output[NonEmptyString],
  //   image: Output[NonEmptyString],
  //   ports: Option[ContainerPortArgs]
  // )
  // object ContainerArgs:
  //   def apply[
  //     F[+_],
  //     In1 <: Output[NonEmptyString] | F[NonEmptyString] | NonEmptyString,
  //     In2 <: Output[NonEmptyString] | F[NonEmptyString] | NonEmptyString
  //   ](using
  //     ctx: Context.Of
  //   )(name: In1, image: In2, ports: ContainerPortArgs | NotProvided): ContainerArgs =
  //     new ContainerArgs(
  //       OutputLift.lift(name),
  //       OutputLift.lift(image),
  //       ports.asOption
  //     )
