# Setup AWS Secrets manager

A simple program that creates an AWS secret and a version under AWS Secrets Manager

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/aws/get-started/begin/)
to get started with Pulumi & AWS.

## Deploying and running the program

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

    ```bash
    pulumi stack init aws-secrets-manager-dev
    ```

2.  Set the AWS region:

    ```bash
    pulumi config set aws:region us-west-2
    ```

3. Create a Pulumi secret that will be saved in the secret manager:

    ```bash
    pulumi config set --secret mySecret
    ```

4. Run `pulumi up` to preview and deploy changes. After the preview is shown
   you will be prompted if you want to continue or not.

    ```bash
    pulumi up
    ```

    After a couple of minutes, your VM will be ready. Your web server will start on port `80`.

5. To see the resources that were created, run `pulumi stack output`:

    ```bash
    pulumi stack output
    ```

6. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

7. To clean up resources, destroy your stack and remove it:

    ```bash
    pulumi destroy
    ```
    ```bash
    pulumi stack rm
    ```