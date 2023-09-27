---
title: Getting started
---
import Version from '@site/src/components/Version';


To start your adventure with infrastructure-as-code with Scala follow these steps:

1. **Install Pulumi CLI**:

   To install the latest Pulumi release, run the following (see full
   [installation instructions](https://www.pulumi.com/docs/reference/install/) for additional installation options):

    ```bash
    curl -fsSL https://get.pulumi.com/ | sh
    ```
2. **Install Scala CLI**:
    
    To install the latest Scala CLI release, see 
    [installation instructions](https://scala-cli.virtuslab.org/install) for installation options.
3. **Install Scala Language Plugin in Pulumi**:
    
    To install the latest Scala Language Plugin release, run the following:
    ```
    pulumi plugin install language scala $version --server github://api.github.com/VirtusLab/besom 
    ```
4. **Create a new project**:
   You can start writing your Besom code at this point, but to help you set up
   Besom comes with [Pulumi templates](./templates).
   
   You can get started with the `pulumi new` command:
    ```bash
    mkdir besom-demo && cd besom-demo
    ```
    ```bash
    pulumi new https://github.com/VirtusLab/besom/tree/develop/template/default
    ```
5. **Deploy to the Cloud**:
   Run `pulumi up` to get your code to the cloud:
    ```bash
    pulumi up
    ```
   This makes all cloud resources declared in your code. Simply make
   edits to your project, and subsequent `pulumi up`s will compute
   the minimal diff to deploy your changes.
6. **Use Your Program**:
   Now that your code is deployed, you can interact with it. In the
   above example, we can find the name of the newly provisioned S3
   bucket:
    ```bash
    $ pulumi stack output bucketName
    ```