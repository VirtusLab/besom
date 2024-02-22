# Amazon EKS Cluster: Hello World!

This example deploys an EKS Kubernetes cluster.

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/aws/get-started/begin/)
to get started with Pulumi & AWS.

## Deploying

1. Create a new stack, which is an isolated deployment target for this example:

   ```bash
   pulumi stack init aws-eks
   ```

2. Set the AWS region:

   ```bash
   pulumi config set aws:region us-west-2
   ```

   We recommend using `us-west-2` to host your EKS cluster as other regions (notably `us-east-1`) may have capacity
   issues that prevent EKS clusters from creating.

3. Stand up the EKS cluster:

   ```bash
   pulumi up
   ```
4. After 10-15 minutes, your cluster will be ready, and the `kubeconfig` JSON you'll use to connect to the cluster will
   be available as an output. You can save this `kubeconfig` to a file like so:

   ```bash
   pulumi stack output kubeconfig > kubeconfig.json
   ```

   Once you have this file in hand, you can interact with your new cluster as usual via `kubectl`:

   ```bash
   kubectl --kubeconfig=./kubeconfig.json get all --all-namespaces
   ```

5. And finally - open the application in your browser to see the running application.

   ```bash
   curl http://$(pulumi stack output serviceHostname)
   ```

6. To clean up resources, destroy your stack and remove it:

   ```bash
   pulumi destroy
   ```
   ```bash
   pulumi stack rm aws-eks
   ```