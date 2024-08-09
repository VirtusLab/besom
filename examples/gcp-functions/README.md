# Google Cloud Functions

An example of deploying an HTTP Google Cloud Function endpoint.

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/intro/cloud-providers/gcp/setup/)
to get started with Pulumi & Google Cloud.

## Running the App

1. Create a new stack:

    ```sh
       $ pulumi stack init dev
    ```

2. Configure your GCP project and region:

    ```sh
    $ pulumi config set gcp:project <projectname>
    $ pulumi config set gcp:region us-west1
    ```

3. Run `pulumi up` to preview and deploy changes:

    ```sh
    $ pulumi up
    ```

4. Check the deployed function endpoint:

    ```sh
    $ pulumi stack output url
    ...
    $ curl "$(pulumi stack output url)"
    Hello World!
    ```

5. Clean up your GCP and Pulumi resources:

    ```sh
    $ pulumi destroy
    ...
    $ pulumi stack rm
    ...
    ```
