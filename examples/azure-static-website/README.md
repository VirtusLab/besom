# Static Website Using Azure Blob Storage and CDN

Based on https://github.com/zemien/static-website-ARM-template

This example
configures [Static website hosting in Azure Storage](https://docs.microsoft.com/en-us/azure/storage/blobs/storage-blob-static-website).

In addition to the Storage itself, a CDN is configured to serve files from the Blob container origin. This may be useful
if you need to serve files via HTTPS from a custom domain (not shown in the example).

## Deploying the App

To deploy your infrastructure, follow the below steps.

### Prerequisites

1. [Install Pulumi](https://www.pulumi.com/docs/get-started/install/)
2. [Configure Azure Credentials](https://www.pulumi.com/docs/intro/cloud-providers/azure/setup/)

## Running the App

1. Create a new stack:

   ```
   $ pulumi stack init dev
   ```

2. Set the Azure region location to use:

   ```
   $ pulumi config set azure-native:location westus
   ```

3. Run `pulumi up` to preview and deploy changes:

   ```
   $ pulumi up
   Previewing changes:
   ...

   Performing changes:
   ...
   Resources:
       + 9 created
   Duration: 2m52s
   ```

4. Check the deployed website endpoint:

   ```
   $ pulumi stack output staticEndpoint

   $ curl "$(pulumi stack output staticEndpoint)"
   <html>
       <body>
           <h1>This file is served from Blob Storage (courtesy of Pulumi!)</h1>
       </body>
   </html>
   ```

5. From there, feel free to experiment. Simply making edits and running `pulumi up` will incrementally update your
   stack.

6. Once you've finished experimenting, tear down your stack's resources by destroying and removing it:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```
