# Stateless Application Using a Kubernetes Deployment

A version of the [Kubernetes Stateless Application Deployment](
https://kubernetes.io/docs/tasks/run-application/run-stateless-application-deployment/) example that uses Pulumi.
This example deploys a replicated Nginx server to a Kubernetes cluster, using Scala and no YAML.

This example is based on a [Pulumi Tutorial available here](https://www.pulumi.com/docs/tutorials/kubernetes/stateless-app/).

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/kubernetes/get-started/begin/)
to get started with Pulumi & Kubernetes.

## Deploying and running the program

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

    ```bash
    pulumi stack init kubernetes-nginx-dev
    ```

2. Run `pulumi up` to preview and deploy changes. After the preview is shown
   you will be prompted if you want to continue or not.

    ```bash
    pulumi config set replicas 2
    pulumi up
    ```

   After a couple of minutes, your deployment will be ready, then you can run commands like `kubectl get pods`
   to see the application's resources.

   The stack's replica count is configurable. By default, it will scale up to three instances, but we can easily change
   that to five, by running the `pulumi config` command followed by another `pulumi up`:

   ```bash
   pulumi config set replicas 5
   pulumi up
   ```

3. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

4. To clean up resources, destroy your stack and remove it:

    ```bash
    pulumi destroy
    ```
    ```bash
    pulumi stack rm kubernetes-nginx-dev
    ```