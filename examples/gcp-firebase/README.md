# Google Firestore database example

This example deploys an Google Firestore database and fill it up with two documents.

## Deploying the App

To deploy your infrastructure, follow the below steps.

### Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/intro/cloud-providers/gcp/setup/)
to get started with Pulumi & Google Cloud.

## Running the App

1. Create a new stack:

   ```bash
   $ pulumi stack init dev
   ```

2. Set the required GCP configuration variables:

    ```bash
    $ pulumi config set gcp:project <YOUR_GCP_PROJECT_HERE>
    ```

3. Stand up the example by invoking pulumi.

     ```bash
       $ pulumi up
     ```

4. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your
   infrastructure.

5. To clean up resources, destroy your stack and remove it:

    ```bash
    $ pulumi destroy
    ```
    ```bash
    $ pulumi stack rm gcp-static-page-dev
    ```