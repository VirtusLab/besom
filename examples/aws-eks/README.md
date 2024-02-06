# Amazon EKS Cluster

This example deploys an EKS Kubernetes cluster with an EBS-backed StorageClass.

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/aws/get-started/begin/)
to get started with Pulumi & AWS.

## Deploying

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

   ```bash
   pulumi stack init aws-eks-dev
   ```

2. Set the AWS region:

   ```bash
   pulumi config set aws:region us-west-2
   ```

   We recommend using `us-west-2` to host your EKS cluster as other regions (notably `us-east-1`) may have capacity issues that prevent EKS
   clusters from creating.
   
   We are tracking enabling the creation of VPCs limited to specific AZs to unblock this in `us-east-1`: pulumi/pulumi-awsx#32

3. Stand up the EKS cluster:

   ```bash
   pulumi up
   ```
4. After 10-15 minutes, your cluster will be ready, and the `kubeconfig` JSON you'll use to connect to the cluster will
   be available as an output. You can save this `kubeconfig` to a file like so:

   ```bash
   pulumi stack output kubeconfig --show-secrets > kubeconfig.json
   ```

    Once you have this file in hand, you can interact with your new cluster as usual via `kubectl`:

   ```bash
   kubectl --kubeconfig=./kubeconfig.json get pods --all-namespaces
   ```

5. To clean up resources, destroy your stack and remove it:

   ```bash
   pulumi destroy
   ```
   ```bash
   pulumi stack rm aws-eks
   ```