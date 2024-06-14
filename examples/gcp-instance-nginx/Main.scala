import besom.*
import besom.api.gcp

@main def main = Pulumi.run {
  val network = gcp.compute.Network("poc-network")

  val firewall = gcp.compute.Firewall(
    name = "poc-firewall",
    gcp.compute.FirewallArgs(
      sourceRanges = List("0.0.0.0/0"),
      network = network.selfLink,
      allows = List(
        gcp.compute.inputs.FirewallAllowArgs(protocol = "tcp", ports = List("22")),
        gcp.compute.inputs.FirewallAllowArgs(protocol = "tcp", ports = List("80"))
      )
    )
  )

  // virtual machine running nginx via a startup script (https://cloud.google.com/compute/docs/startupscript)
  val script =
    """#!/bin/bash
  apt -y update
  apt -y install nginx
  """

  val instanceAddress = gcp.compute.Address("poc-address")

  val instance = gcp.compute.Instance(
    name = "poc-instance",
    gcp.compute.InstanceArgs(
      allowStoppingForUpdate = true,
      machineType = "f1-micro",
      bootDisk = gcp.compute.inputs.InstanceBootDiskArgs(
        initializeParams = gcp.compute.inputs.InstanceBootDiskInitializeParamsArgs(
          image = "ubuntu-os-cloud/ubuntu-1804-bionic-v20200414"
        )
      ),
      networkInterfaces = List(
        gcp.compute.inputs.InstanceNetworkInterfaceArgs(
          network = network.id,
          accessConfigs = List(
            gcp.compute.inputs.InstanceNetworkInterfaceAccessConfigArgs(natIp = instanceAddress.address)
          )
        )
      ),
      metadataStartupScript = script
    )
  )

  // virtual machine with Google's Container-Optimized OS (https://cloud.google.com/container-optimized-os/docs) running nginx as a Docker container
  val containerInstanceAddress = gcp.compute.Address("poc-container-address")
  val containerInstanceMetadataScript =
    """
  spec:
      containers:
          - name: manual-container-instance-1
            image: 'gcr.io/cloud-marketplace/google/nginx1:latest'
            stdin: false
            tty: false
      restartPolicy: Always

  # This container declaration format is not public API and may change without notice. Please
  # use gcloud command-line tool or Google Cloud Console to run Containers on Google Compute Engine.
  """

  val containerInstance = gcp.compute.Instance(
    name = "poc-container-instance",
    gcp.compute.InstanceArgs(
      allowStoppingForUpdate = true,
      machineType = "f1-micro",
      bootDisk = gcp.compute.inputs.InstanceBootDiskArgs(
        initializeParams = gcp.compute.inputs.InstanceBootDiskInitializeParamsArgs(
          image = "cos-cloud/cos-stable-81-12871-69-0"
        )
      ),
      metadata = Map("gce-container-declaration" -> containerInstanceMetadataScript),
      networkInterfaces = List(
        gcp.compute.inputs.InstanceNetworkInterfaceArgs(
          network = network.id,
          accessConfigs = List(
            gcp.compute.inputs.InstanceNetworkInterfaceAccessConfigArgs(natIp = containerInstanceAddress.address)
          )
        )
      ),
      serviceAccount = gcp.compute.inputs.InstanceServiceAccountArgs(
        email = "default",
        scopes = List(
          "https://www.googleapis.com/auth/devstorage.read_only",
          "https://www.googleapis.com/auth/logging.write",
          "https://www.googleapis.com/auth/monitoring.write",
          "https://www.googleapis.com/auth/service.management.readonly",
          "https://www.googleapis.com/auth/servicecontrol",
          "https://www.googleapis.com/auth/trace.append"
        )
      )
    )
  )

  Stack(firewall)
    .exports(
      instanceName = instance.name,
      instanceExternalIp = instanceAddress.address,
      containerInstanceName = containerInstance.name,
      containerInstanceExternalIp = containerInstanceAddress.address
    )
}
