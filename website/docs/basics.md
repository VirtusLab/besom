---
title: Pulumi Basics
---
import Version from '@site/src/components/Version';

Before we dive into the [details of Besom](architecture), let's take a look at the basics of Pulumi.
This page offers an executive summary of [Pulumi's concepts](https://www.pulumi.com/docs/concepts/).

### What is Pulumi?

Pulumi is a modern infrastructure as code platform. It leverages existing programming languages 
and their native ecosystem to interact with cloud resources through the Pulumi SDK.

Pulumi is a registered trademark of [Pulumi Corporation](https://pulumi.com).

### What is Besom?

Besom is a Pulumi SDK for Scala 3. It allows you to use Scala to define your infrastructure
in a type-safe and functional way.

### Concepts

It is important to understand the basic concepts of Pulumi before we dive into the details of Besom.
We strongly advise to get acquainted with [Pulumi's concepts](https://www.pulumi.com/docs/concepts/)
documentation as all of that information applies to Besom as well.

Pulumi uses [programs](#programs) to define [resources](#resources) that are managed using [providers](#providers)
and result in [stacks](#stacks). 

For more detailed information see [how Pulumi works](https://www.pulumi.com/docs/concepts/how-pulumi-works/#how-pulumi-works)
documentation section.

#### Projects

A [Pulumi project](https://www.pulumi.com/docs/concepts/projects/) consists of:
- a [program](#programs) that defines the desired infrastructure
- one or more [stack](#stacks) that defines the target environment for the program
- and metadata on how to run the program, such as [`Pulumi.yaml`](https://www.pulumi.com/docs/concepts/projects/#pulumi-yaml) 
  and [`Pulumi.<stackname>.yaml`](https://www.pulumi.com/docs/concepts/projects/#stack-settings-file) files.

You run the Pulumi CLI command `pulumi up` from within your project directory to deploy your infrastructure.

Project source code is typically stored in a version control system such as Git.
In addition to the project source code, Pulumi also stores a snapshot of the project state in the [backend](#state).

Besom projects are no different. You can use the same project structure and workflow as you would with other Pulumi SDKs.
The only difference is that you use `runtime: scala` in your [`Pulumi.<stackname>.yaml`](https://www.pulumi.com/docs/concepts/projects/project-file/)
with [runtime options](https://www.pulumi.com/docs/concepts/projects/project-file/#runtime-options) being:
- `binary` - a path to pre-built executable JAR
- `use-executor` - force a specific executor path instead of probing the project directory and `PATH`

#### Programs

A Pulumi program, written in a general-purpose programming language, is a collection of [resources](#resources) 
that are deployed to a [stack](#stacks).

#### Stacks

[Pulumi stack](https://www.pulumi.com/docs/concepts/stack/) is a separate, isolated, independently configurable 
instance of a Pulumi [program](#programs), and can be updated and referred to independently. 
A [project](#projects) can have as many stacks as needed.

[Projects](#projects) and [stacks](#stacks) are intentionally flexible so that they can accommodate diverse needs 
across a spectrum of team, application, and infrastructure scenarios. Learn more about organizing your code in 
[Pulumi projects and stacks](https://www.pulumi.com/docs/using-pulumi/organizing-projects-stacks) documentation.

By default, Pulumi creates a stack for you when you start a new project using the `pulumi new` command.
Each stack that is created in a project will have a file named [`Pulumi.<stackname>.yaml`](https://www.pulumi.com/docs/concepts/projects/#stack-settings-file)
in the root of the [project](#projects) directory that contains the [configuration](#configuration) specific to this stack.

The recommended practice is to check stack files into source control as a means of collaboration.
Since secret values are encrypted, it is safe to check in these stack settings.

##### Stack Outputs

Stacks can export values as [Stack Outputs](https://www.pulumi.com/docs/concepts/stack/#outputs).
These outputs are shown by Pulumi CLI commands, and are displayed in the Pulumi Cloud, and can be accessed 
programmatically using [Stack References](#stack-references).

To export values from a stack in Besom, use the [`Pulumi.exports`](exports) function in your program.

##### Stack References

[Stack References](https://www.pulumi.com/docs/concepts/stack/#stackreferences) allow you to use outputs from other 
[stacks](#stacks) in your [program](#programs).

To reference values from another stack, create an instance of the `StackReference` type using the fully qualified 
name of the stack as an input, and then read exported stack outputs by their name.

`StackReference` is not implemented yet in Besom, coming soon.

#### Resources

Resources are the primary [construct of Pulumi](https://www.pulumi.com/docs/concepts/resources/) programs.
Resources represent the fundamental units that make up your infrastructure, such as a compute instance,
a storage bucket, or a Kubernetes cluster.

Resources are defined using a [**resource constructor**](constructors). Each resource in Pulumi has:
- a [logical name and a physical name](https://www.pulumi.com/docs/concepts/resources/names/#resource-names)
  The logical name establishes a notion of identity within Pulumi, and the physical name is used as identity by the provider
- a [resource type](https://www.pulumi.com/docs/concepts/resources/names/#types), which identifies the provider and the kind of resource being created
- [URN](https://www.pulumi.com/docs/concepts/resources/names/#types), which is an automatically constructed globally unique identifier for the resource.

Each resource also can have:
- a set of [arguments](https://www.pulumi.com/docs/concepts/resources/properties/) that define the behavior of the resulting infrastructure
- and a set of [options](https://www.pulumi.com/docs/concepts/options/) that control how the resource is created and managed by the Pulumi engine.

Every is managed by a [provider](https://www.pulumi.com/docs/concepts/resources/providers/) which is a plugin that
provides the implementation details. If not specified explicitly, the default provider is used.
Providers can be configured using [provider configuration](https://www.pulumi.com/docs/concepts/resources/providers/#explicit-provider-configuration).

#### Inputs and Outputs

Inputs and Outputs are the primary [asynchronous data types in Pulumi](https://www.pulumi.com/docs/concepts/inputs-outputs/), 
and they signify values that will be provided by the engine later, when the resource is created and its properties can be fetched.
`Input[A]` type is an alias for `Output[A]` type used by [resource](#resources) arguments.

Outputs are values of type `Output[A]` and behave very much like [monads](https://en.wikipedia.org/wiki/Monad_(functional_programming)). 
This is necessary because output values are not fully known until the infrastructure resource has actually completed 
provisioning, which happens asynchronously after the program has finished executing.

Outputs are used to:
- automatically captures dependencies between [resources](#resources)
- provide a way to express transformations on its value before it's known
- deffer the evaluation of its value until it's known
- track the _secretness_ of its value

Output transformations available in Besom:
- [`map` and `flatMap`](apply_methods) methods take a callback that receives the plain value, and computes a new output
- [lifting](lifting) directly read properties off an output value
- [interpolation](interpolator) concatenate string outputs with other strings directly

To create an output from a plain value, use the `Output` constructor, e.g.:
```scala
val hello = Output("hello")
```

If you have multiple outputs and need to use them together as a tuple you can use the standard 
[`zip`](https://scala-lang.org/api/3.x/scala/collection/View.html#zip-1dd) 
method to combine them into a single output:

```scala
val port: Output[Int] = pod.port
val host: Output[String] = node.hostname
val hello = host.zip(port).map { case (a, b) => s"https://$hostname:$port/" }
```

To access String outputs directly, use the [interpolator](interpolator):

```scala
val port: Output[Int] = pod.port
val host: Output[String] = node.hostname
val https: Output[String] = p"https://$host:$port/api/"
```

We encourage you to learn more about relationship between [resources](#resources) and [outputs](#inputs-and-outputs) 
in the [Resource constructors and asynchronicity](constructors) section.

#### Configuration

[Configuration](https://www.pulumi.com/docs/concepts/config) is a set of key-value pairs that influence 
the behavior of a Pulumi program.

Configuration keys use the format `[<namespace>:]<key-name>`, with a colon delimiting the optional namespace 
and the actual key name. Pulumi automatically uses the current project name from [`Pulumi.yaml`](https://www.pulumi.com/docs/concepts/projects/#pulumi-yaml)
as the default key namespace.

Configuration values can be set in two ways:
- [`Pulumi.<stackname>.yaml`](https://www.pulumi.com/docs/concepts/projects/#stack-settings-file) file
- [`pulumi config`](https://www.pulumi.com/docs/concepts/config/#setting-and-getting-configuration-values) command

##### Accessing Configuration from Code

Configuration values can be [accessed](https://www.pulumi.com/docs/concepts/config/#code) from [programs](#programs) 
using the `Config` object, e.g.:

```scala
val a = config.get("aws:region")
val b = Config("aws").map(_.get("region"))
```

#### Providers

A [resource provider](https://www.pulumi.com/docs/concepts/resources/providers/) is a plugin that
handles communications with a cloud service to create, read, update, and delete the resources you define in 
your Pulumi [programs](#programs).

You import a Provider SDK (e.g. `import besom.api.aws`) library in you [program](#programs), Pulumi passes your code to 
the language host plugin (i.e. `pulumi-language-scala`), waits to be notified of resource registrations, assembles 
a model of your desired [state](#state), and calls on the resource provider (e.g. `pulumi-resource-aws`) to produce that state. 
The resource provider translates those requests into API calls to the cloud service or platform.

Providers can be configured using [provider configuration](https://www.pulumi.com/docs/concepts/resources/providers/#explicit-provider-configuration).

:::tip
It is recommended to [disable default providers](https://www.pulumi.com/blog/disable-default-providers/) 
if not for [all providers, at least for Kubernetes](https://www.pulumi.com/docs/concepts/config#pulumi-configuration-options).
:::

#### State

State is a snapshot of your [project](#projects) [resources](#resources) that is stored in a [backend](https://www.pulumi.com/docs/concepts/state/#deciding-on-a-state-backend) 
with [Pulumi Service](https://www.pulumi.com/docs/intro/cloud-providers/pulumi-service/) being the default.

State is used to:
- track [resources](#resources) that are created by your [program](#programs)
- record the relationship between resources
- store metadata about your [project](#projects) and [stacks](#stacks)
- and store [configuration](#configuration) and secret values
- and store [stack outputs](#stack-outputs)

:::tip
Fore extra curious [here's the internal state schema](https://pulumi-developer-docs.readthedocs.io/en/latest/architecture/deployment-schema.html)
:::