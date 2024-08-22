import besom.*
import besom.api.kubernetesingressnginx as nginx
import besom.api.{kubernetes => k8s}

case class NginxArgs()

case class Nginx private (name: Output[String], namespace: Output[String])(using ComponentBase) extends ComponentResource
    derives RegistersOutputs

object Nginx:
  extension (o: Output[Nginx])
    def name: Output[String]      = o.flatMap(_.name)
    def namespace: Output[String] = o.flatMap(_.namespace)

  def apply(using
    Context
  )(
    name: NonEmptyString,
    args: NginxArgs = NginxArgs(),
    options: ResourceOptsVariant.Component ?=> ComponentResourceOptions = ComponentResourceOptions()
  ): Output[Nginx] =
    component(name, "custom:resource:Nginx", options(using ResourceOptsVariant.Component)) {
      val namespace = "ingress-nginx"
      val nginxNamespace = k8s.core.v1.Namespace(
        name = namespace,
        k8s.core.v1.NamespaceArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(name = namespace)
        )
      )

      val ingressController =
        nginx.IngressController(
          name = s"$name-ingress-nginx",
          nginx.IngressControllerArgs(
            helmOptions = nginx.inputs.ReleaseArgs(
              version = "4.7.1",
              name = namespace,
              namespace = namespace
            )
          ),
          opts = opts(dependsOn = nginxNamespace)
        )

      Nginx(
        name = ingressController.status.name,
        namespace = ingressController.status.namespace
      )
    }
