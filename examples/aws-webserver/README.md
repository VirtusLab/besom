# Web Server Using Amazon EC2

This example deploys a simple AWS EC2 virtual machine running a simple web server.

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/aws/get-started/begin/)
to get started with Pulumi & AWS.

## Deploying and running the program

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

    ```bash
    pulumi stack init aws-webserver-dev
    ```

2. Set the AWS region:

    ```bash
    pulumi config set aws:region us-west-2
    ```

3. Run `pulumi up` to preview and deploy changes. After the preview is shown
   you will be prompted if you want to continue or not.

    ```bash
    pulumi up
    ```

    After a couple of minutes, your VM will be ready. Your web server will start on port `80`.

4. To see the resources that were created, run `pulumi stack output`:

    ```bash
    pulumi stack output
    ```

5. Thanks to the security group making port `80` accessible to the `0.0.0.0/0` CIDR block (all addresses), we can `curl` it:

    ```bash
    curl $(pulumi stack output publicIp)
    ```
    ```
    Hello, World!
    ```

6. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

7. To clean up resources, destroy your stack and remove it:

    ```bash
    pulumi destroy
    ```
    ```bash
    pulumi stack rm
    ```