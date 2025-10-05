# Scala Steward on Google Cloud Run

A Scala application that uses Google Cloud Run Job to
run [Scala Steward](https://github.com/scala-steward-org/scala-steward/tree/main) job with scheduler.

## Prerequisites

1. **Install Pulumi CLI**:

   To install the latest Pulumi release, run the following (see
   [installation instructions](https://www.pulumi.com/docs/reference/install/) for additional installation options):

    ```bash
    curl -fsSL https://get.pulumi.com/ | sh
    ```

2. **Install Scala CLI**:

   To install the latest Scala CLI release, run the following (see
   [installation instructions](https://scala-cli.virtuslab.org/install) for additional installation options):

    ```bash
    curl -sSLf https://scala-cli.virtuslab.org/get | sh
    ```

3. **Install Scala Language Plugin in Pulumi**:

   To install the latest Scala Language Plugin release, run the following:

    ```bash
    pulumi plugin install language scala 0.3.2 --server github://api.github.com/VirtusLab/besom
    ```
   If you not do this, you see this error:\
   `error: failed to load language plugin scala: no language plugin 'pulumi-language-scala' found in the workspace or on your $PATH`


4. [**Install Google CLI**](https://cloud.google.com/sdk/docs/install)


5. **Authenticate and configure GCP**:

   ```bash
   gcloud auth application-default login
   ```

6. **Create access token** and add it to `git:password` (see below) with scope: api, read_repository, write_repository.\
   [Example of creating access token on Gitlab](https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html)

## Deploying and running the program

1. Create a new stack:

   ```bash
   pulumi stack init dev
   ```

2. Set the required configuration variables:

   Set project name:
   ```bash
   pulumi config set gcp:project <value>
   ```
   Set region. Any valid GCP region here but preferred `us-east1, us-west1, or us-central1` because of [**Free Tier
   **](https://cloud.google.com/free/docs/free-cloud-features#free-tier):
   ```bash
   pulumi config set gcp:region us-east1
   ```
   Set password to git repository. Recommended an authentication token:
   ```bash
   pulumi config set git:password <secret-value> --secret
   ```
   Add below lines to `Pulumi.dev.yaml` file. Fill with proper values:
   ```yaml
   scala-steward:git:
     forgeApiHost: <value>
     forgeLogin: <value>
     forgeType: <value>
     gitAuthorEmail: <value>
     password:
       secure: <secret-value> # secret value from above command
   ```
   You may delete config `git:password <secret-value>` from `Pulumi.dev.yaml` created earlier.


3. Run `pulumi up` to preview and deploy changes. After the preview is shown you will be prompted if you want to
   continue or not.

   ```bash
   pulumi up
   ```
   **Warning**: The first launch of the app may take longer, so you may need to change the cloud run job timeout.
   Default is set to 40 min (`2400s`)


4. To see the output that was created, run:

   ```bash
   pulumi stack output
   ```

5. On GCP console check logs and see if scala-steward is working properly.

6. To clean up resources, remove docker images, destroy your stack and remove it:

   ```bash
   pulumi down
   ```
   ```bash
   pulumi stack rm dev
   ```
   If you want to revoke your Application Default Credentials:
   ```bash
   gcloud auth application-default revoke
   ```