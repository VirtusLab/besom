# Azure Container Instances on Linux

Starting point for building web application hosted in Azure Container Instances.

## Deploying the App

To deploy your infrastructure, follow the below steps.

### Prerequisites

1. [Install Pulumi](https://www.pulumi.com/docs/get-started/install/)
2. [Configure Azure Credentials](https://www.pulumi.com/docs/intro/cloud-providers/azure/setup/)

## Running the App

1. Create a new stack:

   ```
   $ pulumi stack init dev
   ```

2. Set the Azure region location to use:

    ```
    $ pulumi config set azure-native:location westus2
    ```

3. Stand up the cluster by invoking pulumi
    ```bash
    $ pulumi up
    ```

4. From there, feel free to experiment. Simply making edits and running `pulumi up` will incrementally update your
   stack.

5. Once you've finished experimenting, tear down your stack's resources by destroying and removing it:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```