package besom.api

import besom.util.*
import besom.internal.*

object aws:

  case class SecurityGroupOptions(name: Output[NonEmptyString], ingress: Output[List[IngressRule]])
  object SecurityGroupOptions:
    def apply(using ctx: Context)(
      name: NonEmptyString | Output[NonEmptyString],
      ingress: List[IngressRule] | Output[List[IngressRule]] | NotProvided
    ): SecurityGroupOptions = new SecurityGroupOptions(name.asOutput(), ingress.asOutput())

  case class IngressRule(protocol: Protocol, fromPort: Int, toPort: Int, cidrBlocks: List[String])

  case class InstanceOptions(
    ami: NonEmptyString | Output[NonEmptyString],
    instanceType: NonEmptyString | Output[NonEmptyString] | NotProvided,
    securityGroups: List[String] | Output[List[String]] | List[Output[String]] | NotProvided
  )
