# Web Server Using Amazon EC2

This example deploys a simple AWS EC2 virtual machine running a simple web server.

## Deploying and running the program

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1.  Set the AWS region:

    Either using an environment variable
    ```bash
    export AWS_REGION=us-west-2
    ```

    Or with the stack config
    ```bash
    pulumi config set aws:region us-west-2
    ```

2. Run `pulumi up` to preview and deploy changes. After the preview is shown
   you will be prompted if you want to continue or not.

    ```bash
    pulumi up
    ```

    After a couple of minutes, your VM will be ready. Your web server will start on port `80`.

3. To see the resources that were created, run `pulumi stack output`:

    ```bash
    pulumi stack output
    ```

4. Thanks to the security group making port `80` accessible to the `0.0.0.0/0` CIDR block (all addresses), we can `curl` it:

    ```bash
    curl $(pulumi stack output publicIp)
    ```
    ```
    Hello, World!
    ```

5. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

6. To clean up resources, destroy your stack and remove it:

    ```bash
    pulumi destroy
    ```
    ```bash
    pulumi stack rm
    ```