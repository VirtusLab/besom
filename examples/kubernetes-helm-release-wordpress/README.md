# Wordpress Helm Chart Deployed Using Helm Release Resource

Uses the Helm Release API to deploy the Wordpress Helm Chart to a Kubernetes cluster.
The Helm Release resource will install the Chart mimicing behavior of the Helm CLI.

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

   We can see here in the `---outputs:---` section that Wordpress was allocated a Cluster IP. It is exported with a
   stack output variable, `frontendIp`. Since this is a Cluster IP, you will need to port-forward
   to the service in order to hit the endpoint at `http://localhost:8080`
   by running the port-forward command specified in `portForwardCommand`.

   You can navigate to the site in a web browser.

3. From there, feel free to experiment. Simply making edits and running `pulumi up` will incrementally update your
   stack.

4. Once you've finished experimenting, tear down your stack's resources by destroying and removing it:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```
   