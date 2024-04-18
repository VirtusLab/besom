# Google Kubernetes Engine (GKE) Cluster

This example deploys an Google Cloud Platform (
GCP) [Google Kubernetes Engine (GKE)](https://cloud.google.com/kubernetes-engine/) cluster.

## Deploying the App

To deploy your infrastructure, follow the below steps.

### Prerequisites

1. [Install Pulumi](https://www.pulumi.com/docs/get-started/install/)
2. [Install Google Cloud SDK (`gcloud`)](https://cloud.google.com/sdk/docs/downloads-interactive)
3. Configure GCP Auth

    * Login using `gcloud`

        ```bash
        $ gcloud auth login
        $ gcloud config set project <YOUR_GCP_PROJECT_HERE>
        $ gcloud auth application-default login
        ```
   > Note: This auth mechanism is meant for inner loop developer
   > workflows. If you want to run this example in an unattended service
   > account setting, such as in CI/CD, please [follow instructions to
   > configure your service account](https://www.pulumi.com/docs/intro/cloud-providers/gcp/setup/). The
   > service account must have the role `Kubernetes Engine Admin` / `container.admin`.

## Running the App

1. Create a new stack:

   ```bash
   $ pulumi stack init dev
   ```

2. Set the required GCP configuration variables:

    ```bash
    $ pulumi config set gcp:project <YOUR_GCP_PROJECT_HERE>
    $ pulumi config set gcp:zone us-west1-a     // any valid GCP Zone here
    ```

3. Stand up the GKE cluster by invoking pulumi.

     ```bash
       $ pulumi up
     ```

4. After 3-5 minutes, your cluster will be ready, and the `kubeconfig` JSON you'll use to connect to the cluster will
   be available as an output.

5. Access the Kubernetes Cluster using `kubectl`

   To access your new Kubernetes cluster using `kubectl`, we need to setup the
   `kubeconfig` file and download `kubectl`. We can leverage the Pulumi
   stack output in the CLI, as Pulumi facilitates exporting these objects for us.

    ```bash
    $ pulumi stack output kubeconfig > kubeconfig
    $ kubectl --kubeconfig=./kubeconfig.json  get all --all-namespaces 
    ```

6. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your
   infrastructure.

7. To clean up resources, destroy your stack and remove it:

    ```bash
    $ pulumi destroy
    ```
    ```bash
    $ pulumi stack rm gcp-static-page-dev
    ```