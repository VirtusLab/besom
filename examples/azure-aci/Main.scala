import besom.*
import besom.api.azurenative

@main def main = Pulumi.run {
  val resourceGroup = azurenative.resources.ResourceGroup("resourceGroup")

  val imageName = "mcr.microsoft.com/azuredocs/aci-helloworld"

  val containerGroup = azurenative.containerinstance.ContainerGroup(
    name = "containerGroup",
    azurenative.containerinstance.ContainerGroupArgs(
      resourceGroupName = resourceGroup.name,
      osType = azurenative.containerinstance.enums.OperatingSystemTypes.Linux,
      containers = List(
        azurenative.containerinstance.inputs.ContainerArgs(
          name = "hello-world",
          image = imageName,
          ports = List(azurenative.containerinstance.inputs.ContainerPortArgs(port = 80)),
          resources = azurenative.containerinstance.inputs.ResourceRequirementsArgs(
            requests = azurenative.containerinstance.inputs.ResourceRequestsArgs(
              cpu = 1.0,
              memoryInGB = 1.5
            )
          )
        )
      ),
      ipAddress = azurenative.containerinstance.inputs.IpAddressArgs(
        ports = List(
          azurenative.containerinstance.inputs.PortArgs(
            port = 80,
            protocol = azurenative.containerinstance.enums.ContainerGroupNetworkProtocol.TCP
          )
        ),
        `type` = azurenative.containerinstance.enums.ContainerGroupIpAddressType.Public
      ),
      restartPolicy = azurenative.containerinstance.enums.ContainerGroupRestartPolicy.Always
    )
  )

  Stack(containerGroup).exports(
    // TODO uncomment when bug https://github.com/VirtusLab/besom/issues/432 will be fixed
    containerIPv4Address = containerGroup.ipAddress.ip
  )
}
