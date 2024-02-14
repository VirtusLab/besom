---
title: Pulumi Basics
---

import Version from '@site/src/components/Version';

Before we dive into the [details of Besom](architecture.md), let's take a look at the basics of Pulumi.
This page offers an executive summary of [Pulumi's concepts](https://www.pulumi.com/docs/concepts/).

## What is Pulumi?

Pulumi is a modern infrastructure as code platform. It leverages existing programming languages
and their native ecosystem to interact with cloud resources through the Pulumi SDK.

Pulumi is a registered trademark of [Pulumi Corporation](https://pulumi.com).

## What is Besom?

Besom is a **Pulumi SDK for Scala 3**. It allows you to use Scala to define your infrastructure
in a type-safe and functional way.

Besom **does NOT depend on Pulumi Java SDK**, it is a completely separate implementation.

:::caution
Please pay attention to your dependencies, **only use `org.virtuslab::besom-*`** and not `com.pulumi:*`.<br/>
:::

## Concepts

It is important to understand the basic concepts of Pulumi before we dive into the details of Besom.
We strongly advise to get acquainted with [Pulumi's concepts](https://www.pulumi.com/docs/concepts/)
documentation as all of that information applies to Besom as well.

Pulumi uses [programs](#programs) to define [resources](#resources) that are managed using [providers](#providers)
and result in [stacks](#stacks).

For more detailed information
see [how Pulumi works](https://www.pulumi.com/docs/concepts/how-pulumi-works/#how-pulumi-works)
documentation section.

### Projects

A [Pulumi project](https://www.pulumi.com/docs/concepts/projects/) consists of:

- a [program](#programs) that defines the desired infrastructure
- one or more [stack](#stacks) that defines the target environment for the program
- and metadata on how to run the program, such
  as [`Pulumi.yaml`](https://www.pulumi.com/docs/concepts/projects/#pulumi-yaml)
  and [`Pulumi.<stackname>.yaml`](https://www.pulumi.com/docs/concepts/projects/#stack-settings-file) files.

You run the Pulumi CLI command `pulumi up` from within your project directory to deploy your infrastructure.

Project source code is typically stored in a version control system such as Git.
In addition to the project source code, Pulumi also stores a snapshot of the project state in the [backend](#state).

Besom projects are no different. You can use the same project structure and workflow as you would with other Pulumi
SDKs.
The only difference is that you use `runtime: scala` in
your [`Pulumi.yaml`](https://www.pulumi.com/docs/concepts/projects/project-file/)
with [runtime options](https://www.pulumi.com/docs/concepts/projects/project-file/#runtime-options) being:

- `binary` - a path to pre-built executable JAR
- `use-executor` - force a specific executor path instead of probing the project directory and `PATH`

A minimal Besom `Pulumi.yaml` project file:

```yaml
name: Example Besom project file with only required attributes
runtime: scala
```

### Programs

A Pulumi program, written in a general-purpose programming language, is a collection of [resources](#resources)
that are deployed to form a [stack](#stacks).

A minimal Besom program consists of:

* `project.scala` - the program dependencies (here we
  use [Scala-CLI directives](https://scala-cli.virtuslab.org/docs/guides/using-directives/))
    ```scala
    //> using scala "3.3.1"
    //> using plugin "org.virtuslab::besom-compiler-plugin:0.1.0"
    //> using dep "org.virtuslab::besom-core:0.1.0"
    ```
* `Main.scala` - the actual program written in Scala
    ```scala
    import besom.*
    
    @main def main = Pulumi.run {
      Stack(
        log.warn("Nothing's here yet, it's waiting for you to write some code!")
      )
    }
    ```

:::tip
Pass [`Context`](context.md) everywhere you are using Besom outside of `Pulumi.run` block with `(using besom.Context)`.
:::

### Stacks

[Pulumi stack](https://www.pulumi.com/docs/concepts/stack/) is a separate, isolated, independently configurable
instance of a Pulumi [program](#programs), and can be updated and referred to independently.
A [project](#projects) can have as many stacks as needed.

[Projects](#projects) and [stacks](#stacks) are intentionally flexible so that they can accommodate diverse needs
across a spectrum of team, application, and infrastructure scenarios. Learn more about organizing your code in
[Pulumi projects and stacks](https://www.pulumi.com/docs/using-pulumi/organizing-projects-stacks) documentation.

By default, Pulumi creates a stack for you when you start a new project using the `pulumi new` command.
Each stack that is created in a project will have a file
named [`Pulumi.<stackname>.yaml`](https://www.pulumi.com/docs/concepts/projects/#stack-settings-file)
in the root of the [project](#projects) directory that contains the [configuration](#configuration-and-secrets) specific to this
stack.

The stack is represented in a Besom program by a `Stack` datatype that user is expected to return from the main `Pulumi.run` function. `Stack` is used to mark resources or values that stack depends on or that user wants to export as stack outputs. You can return a `Stack` that consists of exports only (for instance when everything you depend on is composed into a thing that you export in the final step) using `Stack.export(x = a, y = b)` or a `Stack` that has only dependencies when you don't want to export anything using `Stack(x, y)`. You can also use some resources and export others using `Stack(a, b).export(x = i, y = j)` syntax. 

:::tip
The recommended practice is to **check stack files into source control** as a means of collaboration.<br/>
Since secret values are encrypted, it is safe to check in these stack settings.
:::

##### Stack and project information from code

You can access Pulumi stack and project information from your [program](#programs) context using:
```scala
pulumiProject      // the Pulumi project name
pulumiOrganization // the Pulumi organization name
pulumiStack        // the Pulumi stack name
urn                // the Pulumi stack URN
```

##### Stack Outputs

Stacks can export values as [Stack Outputs](https://www.pulumi.com/docs/concepts/stack/#outputs).
These outputs are shown by Pulumi CLI commands, and are displayed in the Pulumi Cloud, and can be accessed
programmatically using [Stack References](#stack-references).

To export values from a stack in Besom, use the [`Stack.exports`](exports.md) function in your program to assign exported values to the final `Stack` value.

##### Stack References

[Stack Reference](https://www.pulumi.com/docs/concepts/stack/#stackreferences) allows you to use outputs from other [stacks](#stacks) in your [program](#programs).

To reference values from another stack, create an instance of the `StackReference` type using the fully qualified
name of the stack as an input, and then read exported stack outputs by their name.

`StackReference` is not implemented yet in Besom, coming soon.

### Resources

Resources are the primary [construct of Pulumi](https://www.pulumi.com/docs/concepts/resources/) programs.
Resources represent the fundamental units that make up your infrastructure, such as a compute instance,
a storage bucket, or a Kubernetes cluster.

Resources are defined using a [**resource constructor**](constructors.md). Each resource in Pulumi has:

- a [logical name and a physical name](https://www.pulumi.com/docs/concepts/resources/names/#resource-names)
  The logical name establishes a notion of identity within Pulumi, and the physical name is used as identity by the
  provider
- a [resource type](https://www.pulumi.com/docs/concepts/resources/names/#types), which identifies the provider and the
  kind of resource being created
- [Pulumi URN](https://www.pulumi.com/docs/concepts/resources/names/#types), which is an automatically constructed
  globally unique identifier for the resource.

```scala
val redisNamespace = Namespace(s"redis-cluster-namespace-$name")
redisNamespace.id // the Pulumi ID - a physical name
redisNamespace.urn // the Pulumi URN - a globally unique identifier
redisNamespace.pulumiResourceName // the Pulumi resource name
redisNamespace.typeToken // the Pulumi resource type token
redisNamespace.urn.map(_.resourceName) // the logical name
redisNamespace.urn.map(_.resourceType) // the resource type
```

Each resource can also have:

- a set of [arguments](https://www.pulumi.com/docs/concepts/resources/properties/) that define the behavior of the
  resulting infrastructure
- and a set of [options](https://www.pulumi.com/docs/concepts/options/) that control how the resource is created and
  managed by the Pulumi engine.

Every [resource](#resources) is managed by a [provider](#providers) which is a plugin that
provides the implementation details. If not specified explicitly, the default provider is used.
Providers can be configured
using [provider configuration](https://www.pulumi.com/docs/concepts/resources/providers/#explicit-provider-configuration).

Static [get functions](https://www.pulumi.com/docs/concepts/resources/get/) can be used to look up any existing resource that 
is not managed by Pulumi. Here's an example of how to use it:

  ```scala
  @main def main = Pulumi.run {
    val group = aws.ec2.SecurityGroup.get(name = "group", id = "sg-0dfd33cdac25b1ec9")
    ...
  }
  ```

### Inputs and Outputs

Inputs and Outputs are the
primary [asynchronous data types in Pulumi](https://www.pulumi.com/docs/concepts/inputs-outputs/),
and they signify values that will be provided by the engine later, when the resource is created and its properties can
be fetched.
`Input[A]` type is an alias for `Output[A]` type used by [resource](#resources) arguments.

Outputs are values of type `Output[A]` and behave very much
like [monads](https://en.wikipedia.org/wiki/Monad_(functional_programming)).
This is necessary because output values are not fully known until the infrastructure resource has actually completed
provisioning, which happens asynchronously after the program has finished executing.

Outputs are used to:

- automatically captures dependencies between [resources](#resources)
- provide a way to express transformations on its value before it's known
- deffer the evaluation of its value until it's known
- track the _secretness_ of its value

Output transformations available in Besom:

- [`map` and `flatMap`](apply_methods.md) methods take a callback that receives the plain value, and computes a new
  output
- [lifting](lifting.md) directly read properties off an output value
- [interpolation](interpolator.md) concatenate string outputs with other strings directly
- `sequence` method combines multiple outputs into a single output of a list
- `zip` method combines multiple outputs into a single output of a tuple
- `traverse` method transforms a map of outputs into a single output of a map

To create an output from a plain value, use the `Output` constructor, e.g.:

```scala
val hello = Output("hello")
val world = Output.secret("world")
```

To transform an output value, use the `map` and `flatMap` methods, e.g.:

```scala
val hello = Output("hello").map(_.toUpperCase)
val world = Output.secret("world")
val helloWorld: Output[String] = hello.flatMap(h => h + "_" + world.map(_.toUpperCase))
```

If you have multiple outputs of the same type and need to use them together **as a list** you can use
`Output.sequence` method to combine them into a single output:

```scala
val port: Output[String] = pod.name
val host: Output[String] = node.hostname
val hello: Output[List[String]] = List(host, port).sequence // we use the extension method here
```

If you have multiple outputs of different types and need to use them together **as a tuple** you can use the standard
[`zip`](https://scala-lang.org/api/3.x/scala/collection/View.html#zip-1dd) method and pattern matching (`case`) to
combine them into a single output:

```scala
val port: Output[Int] = pod.port
val host: Output[String] = node.hostname
val hello: Output[(String, Int)] = host.zip(port)
val url: Output[String] = hello.map { case (hostname, portValue) => s"https://$hostname:$portValue/" }
```

If you have a map of outputs and need to use them together **as a map** you can use
`Output.traverse` method to combine them into a single output:

```scala
val m: Map[String, Output[String]] = Map(pod.name -> pod.port)
val o: Output[Map[String, String]] = m.traverse // we use the extension method here
```

You can also use `Output.traverse` like that:

```scala
val names: List[String] = List("John", "Paul")
val outputNames: Output[List[String]] = names.traverse(name => Output(name))
```

To access `String` outputs directly, use the [interpolator](interpolator.md):

```scala
val port: Output[Int] = pod.port
val host: Output[String] = node.hostname
val https: Output[String] = p"https://$host:$port/api/"
```

We encourage you to learn more about relationship between [resources](#resources) and [outputs](#inputs-and-outputs)
in the [Resource constructors and asynchronicity](constructors.md) section.

### Configuration and Secrets

[Configuration](https://www.pulumi.com/docs/concepts/config) or [Secret](https://www.pulumi.com/docs/concepts/secrets/)
is a set of key-value pairs that influence the behavior of a Pulumi program.

Configuration or secret keys use the format `[<namespace>:]<key-name>`, with a colon delimiting the optional namespace
and the actual key name. Pulumi automatically uses the current project name
from [`Pulumi.yaml`](https://www.pulumi.com/docs/concepts/projects/#pulumi-yaml) as the default key namespace.

Configuration values can be set in two ways:

- [`Pulumi.<stackname>.yaml`](https://www.pulumi.com/docs/concepts/projects/#stack-settings-file) file
- [`pulumi config set`](https://www.pulumi.com/docs/concepts/config/#setting-and-getting-configuration-values)
  and [`pulumi config set --secret`](https://www.pulumi.com/docs/concepts/secrets/#secrets) commands

##### Accessing Configuration and Secrets from Code

Configuration and secret values can be [accessed](https://www.pulumi.com/docs/concepts/config/#code) from [programs](#programs)
using the `Config.get*` and `Config.require*` method family, e.g.:

```scala
val a: Output[Option[String]] = config.getString("aws:region")
val b: Output[String] = config.requireString("aws:profile")
val c: Output[Option[String]] = Config("aws").map(_.get("region"))
```

If the configuration value is a **secret**, it will be **automatically marked** internally as such and **redacted** in console outputs.

[Structured Configuration](https://www.pulumi.com/docs/concepts/config/#structured-configuration) is also supported
in two flavors: JSON AST (`config.getJson` or `config.requireJson`) or object deserialization (`config.getObject` or `config.requireObject`)
and can be used to read Pulumi configuration in more advanced use cases.

:::note
Secret values are automatically [encrypted and stored](https://www.pulumi.com/docs/concepts/secrets/#configuring-secrets-encryption) in the Pulumi [state](#state).
:::

:::tip
Secrets in Besom differ in behavior from other Pulumi SDKs. In other SDKs, if you try to get a config key that is a
secret, you will obtain it as plaintext (and due to [a bug](https://github.com/pulumi/pulumi/issues/7127) you won't even get a warning).

We choose to do the right thing in Besom and **return all configs as Outputs** so that we can handle failure in pure,
functional way, and **automatically** mark secret values **as [secret Outputs](https://www.pulumi.com/docs/concepts/secrets/#how-secrets-relate-to-outputs)**.
:::

### Providers

A [resource provider](https://www.pulumi.com/docs/concepts/resources/providers/) is a plugin that handles communications with a cloud service to create, read, update, and delete the resources 
you define in your Pulumi [programs](#programs).

You import a Provider SDK (e.g. `import besom.api.aws`) library in you [program](#programs), Pulumi passes your code to
the language host plugin (i.e. `pulumi-language-scala`), waits to be notified of resource registrations, assembles
a model of your desired [state](#state), and calls on the resource provider (e.g. `pulumi-resource-aws`) to produce that
state.
The resource provider translates those requests into API calls to the cloud service or platform.

Providers can be configured
using [provider configuration](https://www.pulumi.com/docs/concepts/resources/providers/#explicit-provider-configuration).

:::tip
It is recommended to [disable default providers](https://www.pulumi.com/blog/disable-default-providers/) if not for [all providers, at least for Kubernetes](https://www.pulumi.com/docs/concepts/config#pulumi-configuration-options).
:::

### State

State is a snapshot of your [project](#projects) [resources](#resources) that is stored in
a [backend](https://www.pulumi.com/docs/concepts/state/#deciding-on-a-state-backend) with [Pulumi Service](https://www.pulumi.com/docs/intro/cloud-providers/pulumi-service/) being the default.

State is used to:

- track [resources](#resources) that are created by your [program](#programs)
- record the relationship between resources
- store metadata about your [project](#projects) and [stacks](#stacks)
- and store [configuration](#configuration-and-secrets) and secret values
- and store [stack outputs](#stack-outputs)

:::note
Fore extra curious [here's the internal state schema](https://pulumi-developer-docs.readthedocs.io/en/latest/architecture/deployment-schema.html)
:::