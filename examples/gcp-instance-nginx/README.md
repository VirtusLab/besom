# Nginx Server Using Compute Engine

Starting point for building the Pulumi nginx server sample in Google Cloud Platform.
This example deploys two GCP virtual machines:

- a virtual machine running nginx via a [startup script](https://cloud.google.com/compute/docs/startupscript)
- a virtual machine running nginx via a Docker container with Google's
  [Container-Optimized OS](https://cloud.google.com/container-optimized-os/docs)

## Running the App

1. Create a new stack:

    ```bash
    $ pulumi stack init dev
    ```

2. Set the required configuration variables for this program:

    ```bash
    pulumi config set gcp:project <projectname>
    pulumi config set gcp:zone us-west1-a # any valid GCP zone here
    ```

3. Run `pulumi up` to preview and deploy changes:

    ```bash
    $ pulumi up
    ```

4. Curl the HTTP server:

    ```bash
    $ curl $(pulumi stack output instanceExternalIp)
    ...
    $ curl $(pulumi stack output containerInstanceExternalIp)
    ...
    ```

5. Destroy the created resources and stacks:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```
