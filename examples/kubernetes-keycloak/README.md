# Kubernetes with Keycloak

This example deploys a Kubernetes application with Keycloak, Postgres and self-signed certificates.

## Prerequisites

Follow the steps in [Pulumi Installation and
Setup](https://www.pulumi.com/docs/get-started/install/) and [Configuring Pulumi
Kubernetes](https://www.pulumi.com/docs/intro/cloud-providers/kubernetes/setup/) to get set up with
Pulumi and Kubernetes.

Add keycloak host (default is `keycloak.localhost`) to DNS.

Example for macOS:

  ```bash
   $ echo '127.0.0.1 keycloak.localhost' | sudo tee -a /etc/hosts
   ```

## Running the App

1. Create a new stack:

   ```bash
   $ pulumi stack init dev
   ```

2. Preview the deployment of the application and perform the deployment:

   ```bash
   $ pulumi up
   ```

3. Get certificate and add to system.

   ```bash
   $ pulumi stack output certificatePem  > tls.crt
   ```
   Example for macOS:
   ```bash
   $ sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ./tls.crt
   ```
4. Login to Keycloak using `keycloakUrl`.
    - default url: `https://keycloak.localhost`
    - default user: `admin`
    - default password: `SUPERsecret`

5. To clean up resources, destroy your stack and remove it:

   ```bash
   pulumi destroy
   ```
   ```bash
   pulumi stack rm dev
   ```
6. Remove certificate (`app-localhost`) from system.

   Example for macOS:
   ```bash
   $ sudo security delete-certificate -c app-localhost
   ```