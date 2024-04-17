import besom.*
import besom.api.gcp

@main def main = Pulumi.run {
  val computeNetwork = gcp.compute.Network(
    name = "network",
    gcp.compute.NetworkArgs(autoCreateSubnetworks = true)
  )

  val computeFirewall = gcp.compute.Firewall(
    name = "firewall",
    gcp.compute.FirewallArgs(
      direction = "INGRESS",
      sourceRanges = List("0.0.0.0/0"),
      network = computeNetwork.selfLink,
      allows = List(
        gcp.compute.inputs.FirewallAllowArgs(
          protocol = "tcp",
          ports = List("22", "80")
        )
      )
    )
  )

  // A simple bash script that will run when the webserver is initalized
  val startupScript =
    """#!/bin/bash
       echo "Hello, World!" > index.html
       nohup python -m SimpleHTTPServer 80 &"""

  val instanceAddr = gcp.compute.Address("address")

  val computeInstance = gcp.compute.Instance(
    name = "instance",
    gcp.compute.InstanceArgs(
      machineType = "f1-micro",
      metadataStartupScript = startupScript,
      bootDisk = gcp.compute.inputs.InstanceBootDiskArgs(
        initializeParams = gcp.compute.inputs.InstanceBootDiskInitializeParamsArgs(
          image = "debian-cloud/debian-9-stretch-v20181210"
        )
      ),
      networkInterfaces = List(
        gcp.compute.inputs.InstanceNetworkInterfaceArgs(
          network = computeNetwork.id,
          accessConfigs = List(
            gcp.compute.inputs.InstanceNetworkInterfaceAccessConfigArgs(
              natIp = instanceAddr.address
            )
          )
        )
      ),
      serviceAccount = gcp.compute.inputs.InstanceServiceAccountArgs(
        scopes = List("https://www.googleapis.com/auth/cloud-platform")
      )
    ),
    opts = opts(dependsOn = computeFirewall)
  )

  Stack.exports(
    instanceName = computeInstance.name,
    instanceIP = instanceAddr.address
  )
}
