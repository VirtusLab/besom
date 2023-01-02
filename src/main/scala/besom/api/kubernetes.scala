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
    opts: ResourceOptions = CustomResourceOptions()
  ): Output[Deployment] =
    ???

  case class DeploymentArgs(spec: DeploymentSpecArgs)

  case class LabelSelectorArgs(
    matchLabels: Output[Map[String, String]]
  )
  object LabelSelectorArgs:
    def apply(
      matchLabels: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided
    )(using ctx: Context): LabelSelectorArgs = new LabelSelectorArgs(matchLabels.asOutputMap)

  case class DeploymentSpecArgs(
    selector: LabelSelectorArgs,
    replicas: Output[Int],
    template: PodTemplateSpecArgs
  )
  object DeploymentSpecArgs:
    def apply(
      selector: LabelSelectorArgs,
      replicas: Int | Output[Int],
      template: PodTemplateSpecArgs
    )(using ctx: Context): DeploymentSpecArgs =
      new DeploymentSpecArgs(selector, replicas.asOutput, template)

  case class PodTemplateSpecArgs(metadata: ObjectMetaArgs, spec: PodSpecArgs)

  case class ObjectMetaArgs(
    labels: Output[Map[String, String]]
  )
  object ObjectMetaArgs:
    def apply(
      labels: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided
    )(using ctx: Context): ObjectMetaArgs = new ObjectMetaArgs(labels.asOutputMap)

  case class PodSpecArgs(containers: List[ContainerArgs])
  object PodSpecArgs:
    def apply(containers: ContainerArgs*)(using ctx: Context): PodSpecArgs =
      new PodSpecArgs(containers.toList)

  case class ContainerArgs(
    name: Output[NonEmptyString],
    image: Output[NonEmptyString],
    ports: Option[ContainerPortArgs]
  )
  object ContainerArgs:
    def apply(
      name: NonEmptyString | Output[NonEmptyString],
      image: NonEmptyString | Output[NonEmptyString],
      ports: ContainerPortArgs | NotProvided = NotProvided
    )(using ctx: Context): ContainerArgs = new ContainerArgs(name.asOutput, image.asOutput, ports.asOption)

  case class ContainerPortArgs(containerPort: Output[Int])
  object ContainerPortArgs:
    def apply(containerPort: Int | Output[Int] | NotProvided)(using ctx: Context): ContainerPortArgs =
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
