import besom.*
import besom.api.kubernetes as k8s

case class IngressArgs(namespaceName: Output[String], secretName: Output[String], keycloakResource: Output[Keycloak])
object IngressArgs:
  extension (o: Output[IngressArgs])
    def namespaceName: Output[String]      = o.flatMap(_.namespaceName)
    def secretName: Output[String]         = o.flatMap(_.secretName)
    def keycloakResource: Output[Keycloak] = o.flatMap(_.keycloakResource)

case class Ingress private (id: Output[String])(using ComponentBase) extends ComponentResource derives RegistersOutputs

object Ingress:
  extension (o: Output[Ingress]) def id: Output[String] = o.flatMap(_.id)

  def apply(using
    Context
  )(
    name: NonEmptyString,
    args: IngressArgs,
    options: ResourceOptsVariant.Component ?=> ComponentResourceOptions = ComponentResourceOptions()
  ): Output[Ingress] =
    component(name, "custom:resource:Ingress", options(using ResourceOptsVariant.Component)) {
      val keycloakIngressRuleArgs =
        k8s.networking.v1.inputs.IngressRuleArgs(
          host = args.keycloakResource.host,
          http = k8s.networking.v1.inputs.HttpIngressRuleValueArgs(
            paths = List(
              k8s.networking.v1.inputs.HttpIngressPathArgs(
                path = "/",
                pathType = "ImplementationSpecific",
                backend = k8s.networking.v1.inputs.IngressBackendArgs(
                  service = k8s.networking.v1.inputs.IngressServiceBackendArgs(
                    name = args.keycloakResource.name,
                    port = k8s.networking.v1.inputs.ServiceBackendPortArgs(
                      number = args.keycloakResource.port
                    )
                  )
                )
              )
            )
          )
        )

      val appIngress = k8s.networking.v1.Ingress(
        name = s"$name-ingress",
        k8s.networking.v1.IngressArgs(
          spec = k8s.networking.v1.inputs.IngressSpecArgs(
            tls = List(
              k8s.networking.v1.inputs.IngressTlsArgs(
                hosts = List(args.keycloakResource.host),
                secretName = args.secretName
              )
            ),
            rules = List(keycloakIngressRuleArgs)
          ),
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName,
            annotations = Map(
              "kubernetes.io/ingress.class" -> "nginx"
            )
          )
        )
      )
      Ingress(
        id = appIngress.id
      )
    }
end Ingress
