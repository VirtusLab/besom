# Host a Static Website on Google Cloud Storage

A static website that uses [Cloud Storage](https://cloud.google.com/storage/)
with [Application Load Balancer](https://cloud.google.com/load-balancing/docs/https/ext-load-balancer-backend-buckets).

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/intro/cloud-providers/gcp/setup/) 
to get started with Pulumi & Google Cloud.

## Deploying and running the program

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

    ```bash
    pulumi stack init gcp-static-page-dev
    ```

2. Set the required configuration variables for this program:

    ```bash
    pulumi config set gcp:project [your-gcp-project-here]
    pulumi config set gcp:zone us-west1-a # any valid GCP zone here
    ```

3. Run `pulumi up` to preview and deploy changes. After the preview is shown
   you will be prompted if you want to continue or not.

    ```bash
    pulumi up
    ```

4. To see the resources that were created, run `pulumi stack output`:

    ```bash
    pulumi stack output
    ```

5. Open the site URL in a browser to see the page:

    ```bash
    open http://$(pulumi stack output websiteIp)
    ```

6. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

7. To clean up resources, destroy your stack and remove it:

    ```bash
    pulumi destroy
    ```
    ```bash
    pulumi stack rm
    ```