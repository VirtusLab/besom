import besom.*
import besom.api.azurenative

@main def main = Pulumi.run {
  // Get the desired username and password for our VM.
  val username = config.requireString("username")
  val password = config.requireString("password")

  // All resources will share a resource group.
  val resourceGroupName = azurenative.resources.ResourceGroup("server-rg").name

  // Create a network for all VMs.
  val virtualNetwork = azurenative.network.VirtualNetwork(
    name = "server-network",
    azurenative.network.VirtualNetworkArgs(
      resourceGroupName = resourceGroupName,
      addressSpace = azurenative.network.inputs.AddressSpaceArgs(
        addressPrefixes = List(
          "10.0.0.0/16"
        )
      )
    )
  )

  // Create a subnet within the Virtual Network.
  val subnet = azurenative.network.Subnet(
    name = "default",
    azurenative.network.SubnetArgs(
      resourceGroupName = resourceGroupName,
      virtualNetworkName = virtualNetwork.name,
      addressPrefix = "10.0.1.0/24"
    )
  )

  // Now allocate a public IP and assign it to our NIC.
  val publicIp = azurenative.network.PublicIpAddress(
    name = "server-ip",
    azurenative.network.PublicIpAddressArgs(
      resourceGroupName = resourceGroupName,
      publicIPAllocationMethod = azurenative.network.enums.IpAllocationMethod.Dynamic
    )
  )

  val networkInterface = azurenative.network.NetworkInterface(
    name = "server-nic",
    azurenative.network.NetworkInterfaceArgs(
      resourceGroupName = resourceGroupName,
      ipConfigurations = List(
        azurenative.network.inputs.NetworkInterfaceIpConfigurationArgs(
          name = "webserveripcfg",
          subnet = azurenative.network.inputs.SubnetArgs(id = subnet.id),
          privateIPAllocationMethod = azurenative.network.enums.IpAllocationMethod.Dynamic,
          publicIPAddress = azurenative.network.inputs.PublicIpAddressArgs(id = publicIp.id)
        )
      )
    )
  )

  val initScript =
    """#!/bin/bash
       |echo "Hello, World!" > index.html
       |nohup python -m SimpleHTTPServer 80 &
       |""".stripMargin

  // Now create the VM, using the resource group and NIC allocated above.
  val vm = azurenative.compute.VirtualMachine(
    name = "server-vm",
    azurenative.compute.VirtualMachineArgs(
      resourceGroupName = resourceGroupName,
      networkProfile = azurenative.compute.inputs.NetworkProfileArgs(
        networkInterfaces = List(
          azurenative.compute.inputs.NetworkInterfaceReferenceArgs(id = networkInterface.id)
        )
      ),
      hardwareProfile = azurenative.compute.inputs.HardwareProfileArgs(
        vmSize = azurenative.compute.enums.VirtualMachineSizeTypes.Standard_A0
      ),
      osProfile = azurenative.compute.inputs.OsProfileArgs(
        computerName = "hostname",
        adminUsername = username,
        adminPassword = password,
        customData = base64.encode(initScript),
        linuxConfiguration = azurenative.compute.inputs.LinuxConfigurationArgs(
          disablePasswordAuthentication = false
        )
      ),
      storageProfile = azurenative.compute.inputs.StorageProfileArgs(
        osDisk = azurenative.compute.inputs.OsDiskArgs(
          createOption = azurenative.compute.enums.DiskCreateOptionTypes.FromImage,
          name = "myosdisk1"
        ),
        imageReference = azurenative.compute.inputs.ImageReferenceArgs(
          publisher = "canonical",
          offer = "UbuntuServer",
          sku = "16.04-LTS",
          version = "latest"
        )
      )
    )
  )

  // TODO uncomment when bug https://github.com/VirtusLab/besom/issues/432 will be fixed
//  val ipAddress =
//    azurenative.network
//      .getPublicIPAddress(
//        azurenative.network.GetPublicIpAddressArgs(
//          resourceGroupName = resourceGroupName,
//          publicIpAddressName = publicIp.name
//        )
//      )
//      .ipAddress

  Stack(publicIp).exports(
    vmName = vm.name
    // TODO uncomment when bug https://github.com/VirtusLab/besom/issues/432 will be fixed
    // ipAddress = ipAddress
  )
}

object base64:
  def encode(v: String): String =
    java.util.Base64.getEncoder.encodeToString(v.getBytes)
