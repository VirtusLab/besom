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

* **[Examples](examples)**: Browse Scala examples across many clouds and scenarios including containers, serverless, and infrastructure.

## <a name="getting-started"></a>Getting Started

1. **Install Pulumi CLI**:

   To install the latest Pulumi release, run the following (see full
   [installation instructions](https://www.pulumi.com/docs/reference/install/) for additional installation options):

    ```bash
    curl -fsSL https://get.pulumi.com/ | sh
    ```

2. **Install Scala CLI**:

    To install the latest Scala CLI release, see 
    [installation instructions](https://scala-cli.virtuslab.org/install) for installation options.

3. **Install Language Plugin in Pulumi**:

    To install the latest Scala Language Plugin release, run the following:

    ```bash
    pulumi plugin install --server github://api.github.com/VirtusLab/besom language scala 0.0.1-beta
    ```

4. **Create a new project**:

   You can start writing your Besom code at this point, but to help you set up
   Besom comes with Pulumi [templates](templates).
   
   You can get started with the `pulumi new` command:

    ```bash
    mkdir besom-demo && cd besom-demo
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
    pulumi stack output bucketName
    ```

To learn more, head over to 
[virtuslab.github.io/besom](https://virtuslab.github.io/besom/) for much more information, including
[tutorials](https://virtuslab.github.io/besom/tutorials/), 
[examples](examples), 
and [programming model concepts](https://virtuslab.github.io/besom/concepts/).

## Explaining the file structure
`Pulumi.yaml` is your main Pulumi file, explained [here](https://www.pulumi.com/docs/concepts/projects/project-file/). 

`project.scala` is the file containing your dependencies for [scala-cli](https://scala-cli.virtuslab.org).

`Main.scala` is the entry point for your Infrastructure as Code. 

Resources created in `Pulumi.run{ ... }` block will be created by Pulumi.

## Tips
- Pass `Context` everywhere you are using Pulumi, for example when you are creating a resource.
- Resources are initialized lazily. To make them appear in your physical infrastructure make sure 
their evaluation is triggered directly or transitively from the main for-comprehension block of your Pulumi program.
- Use whatever scala concepts you are familiar with, infrastructure as code in Besom is still a scala program, 
so you have the full potential of the language to work with.
- Pay attention to the types. You will be instantiating case classes to pass parameters, note their package of origin.

## Contributing

Visit [CONTRIBUTING.md](CONTRIBUTING.md) for information on building Besom from source or contributing improvements.