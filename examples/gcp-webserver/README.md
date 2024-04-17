# Web Server Using Compute Engine

Starting point for building the Pulumi web server sample in Google Cloud.

## Running the App

1. Create a new stack:

   ```
   $ pulumi stack init dev
   ```

2. Configure the project:

   ```
   $ pulumi config set gcp:project YOURGOOGLECLOUDPROJECT
   $ pulumi config set gcp:zone us-central1-a
   ```

3. Run `pulumi up` to preview and deploy changes.


4. Curl the HTTP server:

   ```
   $ curl $(pulumi stack output instanceIP)
   Hello, World!
   ```

5. SSH into the server:

   ```
   $ gcloud compute ssh $(pulumi stack output instanceName)
   ```

6. Cleanup

    ```
    $ pulumi destroy
    $ pulumi stack rm
    ```
   