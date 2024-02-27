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

   To install the latest Scala CLI release, run the following (see 
   [installation instructions](https://scala-cli.virtuslab.org/install) for additional installation options):

    ```bash
    curl -sSLf https://scala-cli.virtuslab.org/get | sh
    ```

3. **Install Scala Language Plugin in Pulumi**:

    To install the latest Scala Language Plugin release, run the following:

    ```bash
    pulumi plugin install language scala 0.2.2 --server github://api.github.com/VirtusLab/besom
    ```

4. **Create a new project**:

   You can start writing your Besom code at this point, but to help you set up
   Besom comes with [Pulumi templates](./templates.md).
   
   You can get started with the `pulumi new` command:

    ```bash
    mkdir besom-demo && cd besom-demo
    ```
    ```bash
    pulumi new https://github.com/VirtusLab/besom/tree/v0.2.2/templates/aws
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
    pulumi stack output bucketName
    ```

7. **Destroy your Resources**:

   After you're done, you can remove all resources created by your program:

    ```bash
    pulumi destroy -y
    ```

## Choice of build tool and IDE
---

Besom uses [Scala-CLI](https://scala-cli.virtuslab.org/) for project compilation and execution.

To set up IDE support for an infrastructural project using Besom execute this command 
inside the directory in which Besom project files exist:
```bash
scala-cli setup-ide .
```
As a result of this command, a `.bsp` directory will be created inside the project directory.

When opened, both [Intellij IDEA](https://www.jetbrains.com/idea/) 
and [Metals](https://scalameta.org/metals/) should automatically recognize 
the project and set up the IDE accordingly.

[sbt](https://www.scala-sbt.org/), [gradle](https://gradle.org/) and [maven](https://maven.apache.org/) are also supported out-of-the-box,
but are **not recommended** due to slower iteration speed. 
Use of `sbt`, `gradle` or `mvn` support is suggested for situations where managed infrastructure
is being added to an already existing project that uses sbt as the main build tool.

IDE setup for `sbt`, `gradle` or `mvn` works automatically with both Intellij IDEA and Metals.

[Mill](https://mill-build.com/) is not yet supported.

