# Besom
**Besom** - a broom made of twigs tied around a stick. 
Brooms and besoms are used for protection, to ward off evil spirits, and cleansing of ritual spaces. 
Also, Besom is Scala SDK that allows you to write Pulumi programs in Scala, incidentally.

![Besom logo](./website/static/img/Besom_logo_full_color.png)

**Besom Scala SDK for Pulumi** lets you leverage the full power of [Pulumi Infrastructure as Code Platform](https://pulumi.com) 
using the Scala programming language. Scala support is currently in **Public Beta**.

**Pulumi** is a registered trademark of [Pulumi Corporation](https://pulumi.com).

## Welcome

* **[Get Started with Besom](#getting-started)**: Deploy a simple application in AWS, Azure, Google Cloud or Kubernetes using Besom to describe the desired infrastructure using Scala.

* **[Besom Docs](https://virtuslab.github.io/besom/)**: Learn about Besom concepts, follow user-guides, and consult the reference documentation.

* **[Examples](https://github.com/VirtusLab/besom/tree/v0.2.2/examples)**: Browse Scala examples across many clouds and scenarios including containers, serverless,
  and infrastructure.

## <a name="getting-started"></a>Getting Started

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
   Besom comes with [Pulumi templates](./templates).
   
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

To learn more, head over to 
[virtuslab.github.io/besom](https://virtuslab.github.io/besom/) for much more information, including
[tutorial](https://virtuslab.github.io/besom/docs/tutorial),
[examples](https://github.com/VirtusLab/besom/tree/v0.2.2/examples),
and [architecture and programming model concepts](https://virtuslab.github.io/besom/docs/architecture).

## Explaining the project structure
`Pulumi.yaml` is your main Pulumi file, explained [here](https://www.pulumi.com/docs/concepts/projects/project-file/). 

`project.scala` is the file containing your dependencies for [Scala-CLI](https://scala-cli.virtuslab.org).

`Main.scala` is the entry point for your Infrastructure as Code. 

Resources created in `Pulumi.run { ... }` block will be created by Pulumi.

A simple example using Scala CLI:
```scala
//> using scala "3.3.1"
//> using plugin "org.virtuslab::besom-compiler-plugin:0.2.2"
//> using dep "org.virtuslab::besom-core:0.2.2"
//> using dep "org.virtuslab::besom-aws:6.23.0-core.0.2"

import besom.*
import besom.api.aws

@main def run = Pulumi.run {
   val bucket = aws.s3.Bucket("my-bucket")

   Stack.exports(
      bucketName = bucket.bucket
   )
}
```

> [!NOTE]
> Please pay attention to your dependencies, **only use `org.virtuslab::besom-*`** and not `com.pulumi:*`.
> Besom **does NOT depend on Pulumi Java SDK**, it is a completely separate implementation.

## Tips
- Whenever you use Besom outside the `Pulumi.run` block, pass [`Context`](https://virtuslab.github.io/besom/docs/context) with `(using besom.Context)`
- Resources are initialized lazily. To make them appear in your physical infrastructure make sure 
their evaluation is triggered directly or transitively from the main for-comprehension block of your Pulumi program.
- Use whatever Scala concepts you are familiar with, infrastructure as code in Besom is still a Scala program, 
so you have the full potential of the language to work with.
- Pay attention to the types. You will be instantiating case classes to pass parameters, note their package of origin.

## Requirements

- JDK 11 or higher is required
- Scala 3.3.1 or higher is required

Scala CLI is the recommended build tool, other tools are also
supported. Besom will recognize Scala CLI and SBT programs 
and automatically recompile them without any further configuration. 
The supported versions are:

- Scala CLI 1.0.4 or higher
- SBT 1.9.6 or higher
- Apache Maven 3.8.4 or higher
- Gradle Build Tool 7.4 or higher

Other build tools are supported via the `runtime.options.binary`
configuration option that can point to a pre-built jar in
`Pulumi.yaml`, e.g.:

```yaml
name: myproject
runtime:
  name: scala
  options:
    binary: target/myproject-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Choice of build tool and IDE

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

## Contributing

Visit [CONTRIBUTING.md](CONTRIBUTING.md) for information on building Besom from source or contributing improvements.