# Kubernetes application using plain YAML files

Uses plain YAML files to deploy Nginx application to a Kubernetes cluster.

## Prerequisites

Follow the steps in [Pulumi Installation and
Setup](https://www.pulumi.com/docs/get-started/install/) and [Configuring Pulumi
Kubernetes](https://www.pulumi.com/docs/intro/cloud-providers/kubernetes/setup/) to get set up with
Pulumi and Kubernetes.

## Running the App

1. Create a new stack:

   ```sh
   $ pulumi stack init
   Enter a stack name: dev
   ```

2. Preview the deployment of the application and perform the deployment:

   ```sh
   $ pulumi up
   ```

3. Since this is a Cluster IP, you will need to port-forward
   to the service in order to hit the endpoint at `http://localhost:8080`
   by running the port-forward command:

   ```sh
   $ kubectl port-forward svc/nginx-service 8080:80
   ```

4. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your
   infrastructure.

5. Once you've finished experimenting, tear down your stack's resources by destroying and removing it:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```