# Docker multi-container example

This example Pulumi application runs two containers locally, one Redis container 
and one built from the application in the `app` folder. 

The application queries the Redis database and returns the number of times the page has been viewed.

## Prerequisites

To run this example, make sure [Docker](https://docs.docker.com/engine/installation/) is installed and running.

## Building the program

1. Build the Docker image for the app:

    ```bash
    scala-cli --power package --docker app --docker-image-repository app
    ```

## Deploying and running the program

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

    ```bash
    pulumi -C infra stack init docker-multi-container-app-dev
    ```

2. Run `pulumi up` to preview and deploy changes. After the preview is shown
   you will be prompted if you want to continue or not.

    ```bash
    pulumi -C infra up
    ```

3. To see the resources that were created, run `pulumi stack output`:

    ```bash
    pulumi -C infra stack output
    ```

4. Open the site URL in a browser to see the page:

    ```bash
    open $(pulumi -C infra stack output url)
    ```

5. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

6. To clean up resources, destroy your stack and remove it:

    ```bash
    pulumi -C infra destroy
    ```
    ```bash
    pulumi -C infra stack rm docker-multi-container-app-dev
    ```
    ```bash
    docker rmi app
    ```