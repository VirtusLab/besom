import besom.*
import besom.api.docker
import docker.inputs.*

@main def main = Pulumi.run {
  // Set defaults for redis
  val redisPort = 6379
  val redisHost = "redisdb"

  // Create docker network
  val network = docker.Network(
    "network",
    docker.NetworkArgs(
      name = "services"
    )
  )

  // Get redis image
  val redisImage = docker.RemoteImage(
    "redisImage",
    docker.RemoteImageArgs(
      name = "redis:6.2",
      keepLocally = true
    )
  )

  // Run redis image
  val redisContainer = docker.Container(
    "redisContainer",
    docker.ContainerArgs(
      image = redisImage.imageId,
      ports = List(
        ContainerPortArgs(
          internal = redisPort,
          external = redisPort
        )
      ),
      networksAdvanced = List(
        ContainerNetworksAdvancedArgs(
          name = network.name,
          aliases = Output(redisHost).map(List(_))
        )
      )
    )
  )

  // Set external port for app url
  val appPort = 3000

  // Run container from local app image
  val appContainer = docker.Container(
    "appContainer",
    docker.ContainerArgs(
      image = "app",
      ports = List(
        ContainerPortArgs(
          internal = appPort,
          external = appPort
        )
      ),
      envs = List(
        p"APP_PORT=${appPort}",
        p"REDIS_HOST=${redisHost}",
        p"REDIS_PORT=${redisPort}"
      ),
      networksAdvanced = List(
        ContainerNetworksAdvancedArgs(
          name = network.name
        )
      )
    ),
    opts(
      dependsOn = redisContainer
    )
  )

  Stack(redisImage, redisContainer, appContainer).exports(
    redis = p"redis://${redisHost}:${redisPort}",
    url = p"http://localhost:${appPort}"
  )
}
