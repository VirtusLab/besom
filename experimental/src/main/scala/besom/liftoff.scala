import besom.*, api.k8s, k8s.*

@main
def main(): Unit = Pulumi.run {
  val pod = k8s.pod(
    "app",
    PodArgs(
      kind = "Pod",
      apiVersion = "v1",
      metadata = ObjectMetaArgs(
        name = "app",
        labels = Map("app" -> "nginx")
      ),
      spec = PodSpecArgs(
        containers = ContainerArgs(
          name = "nginx",
          image = "nginx",
          ports = List(
            ContainerPortArgs(name = "http", containerPort = 80)
          )
        ) :: Nil
      )
    )
  )

  for {
    nginx   <- pod
    exports <- Pulumi.exports("name" -> nginx.metadata.map(_.flatMap(_.name)))
  } yield exports
}
