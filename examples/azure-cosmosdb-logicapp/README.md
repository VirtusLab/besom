# Azure Cosmos DB, an API Connection, and a Logic App

With the native Azure provider we can directly use the Azure resource manager API to define API connections and linking
it to a logic app. The resulting experience is much faster in comparison to performing the same operation through ARM
templates.

## Deploying the App

To deploy your infrastructure, follow the below steps.

### Prerequisites

1. [Install Pulumi](https://www.pulumi.com/docs/get-started/install/)
2. [Configure Azure Credentials](https://www.pulumi.com/docs/intro/cloud-providers/azure/setup/)

### Steps

1. Create a new stack, which is an isolated deployment target for this example:

    ```bash
    $ pulumi stack init dev
    ```

2. Set the Azure region location to use:

    ```bash
    $ pulumi config set azure-native:location westus2
    ```

3. Stand up the cluster by invoking pulumi
    ```bash
    $ pulumi up
    ```

4. At this point, you have a Cosmos DB collection and a Logic App listening to HTTP requests. You can trigger the Logic
   App with a `curl` command:

    ```bash
    $ curl -X POST "$(pulumi stack output endpoint)" -d '"Hello World"' -H 'Content-Type: application/json'
    ````
   The POST body will be saved into a new document in the Cosmos DB collection.

5. From there, feel free to experiment. Simply making edits and running `pulumi up` will incrementally update your
   stack.

6. Once you've finished experimenting, tear down your stack's resources by destroying and removing it:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```
