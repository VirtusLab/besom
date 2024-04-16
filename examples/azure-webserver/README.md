# Web Server Using Azure Virtual Machine

This example provisions a Linux web server in an Azure Virtual Machine and gives it a public IP address.

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

2. Configure the app deployment. The username and password here will be used to configure the Virtual Machine. The
   password must adhere to the [Azure restrictions on VM passwords](
   https://docs.microsoft.com/en-us/azure/virtual-machines/windows/faq#what-are-the-password-requirements-when-creating-a-vm).

   The supplied password must be between 6-72 characters long and must satisfy at least 3 of password complexity
   requirements from the following:
   - Contains an uppercase character
   - Contains a lowercase character
   - Contains a numeric digit
   - Contains a special character
   - Control characters are not allowed

   ```
   $ pulumi config set azure-native:location westus    # any valid Azure region will do
   $ pulumi config set username webmaster
   $ pulumi config set password --secret <your-password>
   ```

   Note that `--secret` ensures your password is encrypted safely.

3. Stand up the cluster by invoking pulumi
    ```bash
    $ pulumi up
    ```

4. Get the IP address of the newly-created instance from the stack's outputs:

   Go to azure portal to service `Virtual Machines`. 
   Find newly created virtual machine and get `Public IP address` from it.

5. Check to see that your server is now running:

    ```
    $ curl http://<Public IP address from virtual machine>
    Hello, World!
    ```
6. From there, feel free to experiment. Simply making edits and running `pulumi up` will incrementally update your
   stack.

7. Once you've finished experimenting, tear down your stack's resources by destroying and removing it:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```