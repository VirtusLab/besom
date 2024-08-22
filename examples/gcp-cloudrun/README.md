# Scala on Google Cloud Run

A Scala application that uses Google Cloud Run.

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/intro/cloud-providers/gcp/setup/) 
to get started with Pulumi & Google Cloud.

## Deploying and running the program

Note: some values in this example will be different from run to run.
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

   ```bash
   pulumi -C infra stack init gcp-cloudrun
   ```

2. Set the required configuration variables for this program:

   ```bash
   pulumi -C infra config set gcp:project $(gcloud config get-value project)
   pulumi -C infra config set gcp:region us-west1 # any valid GCP region here
   ```

3. Authenticate to GCR with application-default credentials and Docker:

   ```bash
   gcloud auth application-default login # this is required for pulumi to reach GCP
   gcloud auth configure-docker # this is required to push the container
   ```

4. Run `pulumi up` to preview and deploy changes. After the preview is shown
   you will be prompted if you want to continue or not.

   ```bash
   pulumi -C infra up
   ```

5. To see the resources that were created, run `pulumi stack output`:

   ```bash
   pulumi -C infra stack output
   ```

6. Open the site URL in a browser to see the page:

   ```bash
   open $(pulumi -C infra stack output serviceUrl)
   ```

7. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

8. To clean up resources, remove docker images, destroy your stack and remove it:

   ```bash
   gcloud container images delete $(pulumi -C infra stack output dockerImage)
   gcloud artifacts packages delete --repository gcr.io --location us gcp-cloudrun/app
   ```
   ```bash
   docker rmi $(pulumi -C infra stack output dockerImage)
   ```
   ```bash
   pulumi -C infra destroy
   ```
   ```bash
   pulumi -C infra stack rm gcp-cloudrun
   ```
