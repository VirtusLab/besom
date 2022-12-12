package besom.api

import besom.util.*
import besom.internal.*

enum Protocol:
  case TCP
  case UDP

object aws:

  case class SecurityGroupOptions[F[+_]](name: Output[F, NonEmptyString], ingress: Output[F, List[IngressRule]])
  object SecurityGroupOptions:
    def apply[F[+_]](
        name: NonEmptyString | Output[F, NonEmptyString],
        ingress: List[IngressRule] | Output[F, List[IngressRule]] | NotProvided
    )(using ctx: Context[F]): SecurityGroupOptions[F] =
      given Monad[F] = ctx.monad
      ???

  case class IngressRule(protocol: Protocol, fromPort: Int, toPort: Int, cidrBlocks: List[String])

  case class InstanceOptions[F[+_]](
      ami: Output[F, NonEmptyString],
      instanceType: Output[F, NonEmptyString],
      securityGroups: Output[F, List[String]]
  )
