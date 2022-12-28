package besom.api

import besom.util.*
import besom.internal.*

object aws:

  case class SecurityGroupOptions[F[+_]](name: Output[F, NonEmptyString], ingress: Output[F, List[IngressRule]])
  object SecurityGroupOptions:
    def apply(using ctx: Context)(
      name: NonEmptyString | Output[ctx.F, NonEmptyString],
      ingress: List[IngressRule] | Output[ctx.F, List[IngressRule]] | NotProvided
    ): SecurityGroupOptions[ctx.F] = new SecurityGroupOptions(name.asOutput, ingress.asOutput)

  case class IngressRule(protocol: Protocol, fromPort: Int, toPort: Int, cidrBlocks: List[String])

  case class InstanceOptions[F[+_]](
    ami: NonEmptyString | Output[F, NonEmptyString],
    instanceType: NonEmptyString | Output[F, NonEmptyString] | NotProvided,
    securityGroups: List[String] | Output[F, List[String]] | List[Output[F, String]] | NotProvided
  )
