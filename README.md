# Besom
**Besom** - a broom made of twigs tied around a stick. 
Brooms and besoms are used for protection, to ward off evil spirits, and cleansing of ritual spaces. 
Also, an experimental pulumi-scala implementation, incidentally.

![Besom logo](./website/static/img/Besom_logo_full_color.png)

## Getting started

### Publish necessary packages
Prerequisites: 
[pulumi](https://www.pulumi.com/docs/install/), 
[just](https://github.com/casey/just#installation), 
[scala-cli](https://scala-cli.virtuslab.org/install/), 
java,
[go](https://go.dev/doc/install),
[protoc](https://grpc.io/docs/protoc-installation/),
git, unzip,

Publish locally and install necessary Besom packages:
```bash
just publish-local-core
just publish-local-compiler-plugin
just install-language-plugin
```

### Publish additional SDKs
You have to generate an SDK for a provider of your choice, to do that run: 
```bash
just generate-provider-sdk ${provider_name} ${provider_version}
just publish-local-provider-sdk ${provider_name} ${provider_version}
```

e.g.:

```bash
just generate-provider-sdk kubernetes 4.2.0
just publish-local-provider-sdk kubernetes 4.2.0
```

### Initialize your code with a template
You can start writing your code at this point but to help you set up 
Besom comes with Pulumi templates. 
More information about templates in [./template/README](./template/README)

To initialize your project with chosen template run this in an empty folder:
```shell
pulumi new ${path_to_the_template}
```
e.g.:
```shell
cd ..
mkdir infra
cd infra
pulumi new ../besom/template/kubernetes
```

## Explaining the file structure
`Pulumi.yaml` is your main Pulumi file, explained [here](https://www.pulumi.com/docs/concepts/projects/project-file/). 

`project.scala` is the file containing your dependencies.

`Main.scala` is the entry point for your infrastructure as code. 
Resources created in `Pulumi.run{ ... }` block will be created by Pulumi.

## Setting up the code editor

Both IDEs support rely on BSP.

### BSP setup
Build experimental `scala-compose` and place on `$PATH`:
```
git clone git@github.com:VirtusLab/scala-compose.git
cd scala-compose
cat << EOF | git apply
> diff --git a/project/publish.sc b/project/publish.sc
> index e00f81ca..619d4c99 100644
> --- a/project/publish.sc
> +++ b/project/publish.sc
> @@ -113,8 +113,9 @@ def finalPublishVersion = {
>      }
>    else
>      T {
> -      val state = VcsVersion.vcsState()
> -      computePublishVersion(state, simple = true)
> +      // val state = VcsVersion.vcsState()
> +      // computePublishVersion(state, simple = true)
> +      "1.0.4"
>      }
>  }
>
> EOF
./mill -i show scala-compose.nativeImage
cp out/scala-compose/base-image/nativeImage.dest/scala-cli ~/bin/scala-compose
```

Use `scala-compose` in `besom` directory:
```bash
scala-compose setup-ide --conf-dir .
```

### IntelliJ setup
IntelliJ support is experimental.

1. Make sure you have the latest IntelliJ
2. Install Scala plugin and set update chanel to "Nightly Builds"
3. Use BSP ([documentation](https://www.jetbrains.com/help/idea/bsp-support.html))

Make sure you have `.bsp` directory before you open the project in IntelliJ.

Additionally, please set `scalafmt` as the formatter.

### VSCode setup

If you are using VSCode:
1. Install [Metals](https://scalameta.org/metals/docs/editors/vscode#installation)
2. Open the project in Metals.

Make sure you have `.bsp` directory before you open the project in IntelliJ.

This might not be enough if your infrastructure is just a part (a module) of your existing Scala project. 
For this to work you have to make your build tool aware of the infrastructure code, 
for **sbt** create a corresponding module: 
   ```scala
lazy val infra = project.in(file("infrastructure")).settings(
   libraryDependencies ++= Seq(
   "org.virtuslab" %% "besom-kubernetes" % "0.0.1-beta", // or any other sdk you are using
   "org.virtuslab" %% "besom-core" % "0.0.1-beta"
   ))
   ```
This just informs your IDE about the existence of the infrastructure module,
DO NOT remove dependencies from `project.scala`, because they are necessary in both places.

## Tips
- Pass `Context` everywhere you are using Pulumi, for example when you are creating a resource.
- Resources are initialized lazily. To make them appear in your physical infrastructure make sure 
their evaluation is triggered directly or transitively from the main for-comprehension block of your Pulumi program.
- Use whatever scala concepts you are familiar with, infrastructure as code in Besom is still a scala program, 
so you have the full potential of the language to work with.
- Pay attention to the types. You will be instantiating case classes to pass parameters, note their package of origin.