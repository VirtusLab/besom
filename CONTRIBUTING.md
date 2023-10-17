# Contributing to Besom

Thank you for contributing to help make Besom better. We appreciate the help!

## Code of Conduct

Please make sure to read and observe our
[Contributor Code of Conduct](CODE-OF-CONDUCT.md).

## Communications

We discuss features and file bugs on GitHub via
[Issues](https://github.com/VirtusLab/besom/issues).

### Issues

Feel free to pick up any existing issue that looks interesting to you
or fix a bug you stumble across while using Besom. 
No matter the size, we welcome all improvements.

Before investing a lot of time, please let us know, so we can discuss the issue together.

### Feature Work

For larger features, we'd appreciate it if you open a
[new issue](https://github.com/VirtusLab/besom/issues/new)
before investing a lot of time, so we can discuss the feature together.

Please also be sure to browse
[current issues](https://github.com/VirtusLab/besom/issues)
to make sure your issue is unique, to lighten the triage burden on our maintainers.

## Branching and versioning strategy

We mostly follow the Pulumi strategy:
- `main` branch contains current `*-SNAPSHOT` version
- `vX.Y.Z` tag marks the `X.Y.Z` release
- `release/vX.Y.Z` branch contains the `X.Y.Z` release
- PRs must have a prefix with the **name of the author and issue number** e.g. `pprazak/123-fix-bug`

> [!NOTE]
> Please make sure to **tag first** before creating a release branch. 

Versioning is done using [Semantic Versioning](https://semver.org/), with following additions:
- `x.y.z` for core version, where:
  - `x` no guarantees are made about compatibility,
  - `y` should not break source compatibility, 
  - `z` should not break binary compatibility
- `a.b.c-core.x.y` for provider version, where `a.b.c` is the schema version
- `*-SNAPSHOT` versions are used for development versions

## Developing

### Setting up your development environment

You will want to install the following on your machine:

- [Pulumi CLI](https://www.pulumi.com/docs/install/) 3.30.0 or higher
- [Scala-CLI](https://scala-cli.virtuslab.org/install/) 1.0.4 or higher
- JDK 11 or higher
- Scala 3.3.1 or higher
- Go 1.20 or higher
- [protoc](https://grpc.io/docs/protoc-installation/) 24.3 or higher
- [just](https://github.com/casey/just#installation) 1.14.0 or higher
- git 2.37.1 or higher
- unzip
- coursier

#### Mac OS

```bash
brew install pulumi/tap/pulumi
brew install Virtuslab/scala-cli/scala-cli
brew install coursier/formulas/coursier
brew install just
brew install java11
brew install sbt
brew install go
brew install git
brew install unzip
```

### Preparing a pull request

1. Ensure running `just` passes with no issues.
2. Ensure the branch name is prefixed with your name and contains issue number, e.g. `johndoe/123-fix-bug`.

### Understanding the repo structure

- `core` contains the core of Scala SDK for Pulumi
- `besom-cats` contains the cats effect integration for Scala SDK for Pulumi
- `besom-zio` contains the ZIO integration for Scala SDK for Pulumi
- `codegen` contains Scala code generation fo Besom (e.g. used for provider SDKs)
- `proto` contains a copy of Pulumi protobuf definitions
- `compiler-plugin` contains the compiler plugin for Scala SDK for Pulumi
- `language-plugin` contains the Golang language host provider plugin for Scala SDK for Pulumi
- `templates` define starter project templates
- `examples` contains examples of using Besom

### Working with local dependencies

There are Just targets that help rebuild all Scala and Go packages from
source. For correct cross-referencing in examples and tests, Scala
packages need to be installed into the local `~/.ivy` repo.

The targets do not yet understand dependencies accurately, so you may
need to re-run to make sure changes to Besom SDK or providers are
rebuilt.

As for Go changes, Pulumi CLI will respect the version of
`pulumi-language-scala` it finds in `PATH` over the default version it
ships with. When testing changes to the language provider, you
therefore might need to manipulate `PATH` to prefer the local version.

#### Publish necessary packages

Publish locally and install necessary Besom packages:
```bash
just publish-local-core
just publish-local-compiler-plugin
just install-language-plugin
```

#### Publish additional SDKs

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

### Working with published dependencies

Release builds of the Besom SDK are published to Maven Central 
as `org.virtuslab::besom-core:x.x.x`.

### Adding examples and testing them locally

Every example is a valid Pulumi program that can be tested by manually
doing `pulumi up` in the right folder.

```
cd exmaples/<example-name>
pulumi up
```

Here is a Just helper to run the automated testing:

```
just test-example aws-scala-webserver
```

### Testing templates locally

See `templates/README.md` on how to manually test template changes
locally.

Similarly to examples, Just helper targets are provided to
automatically test templates, for example to test
`templates/default` run:

```
just test-template default
```

## Setting up the code editor

Both IDEs support rely on BSP and is experimental.

### BSP setup with `scala-compose`

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
3. Use [BSP with `scala-cli`](https://scala-cli.virtuslab.org/docs/cookbooks/intellij-multi-bsp) (also see [IntelliJ documentation](https://www.jetbrains.com/help/idea/bsp-support.html))

To make sure you have `.bsp` directories, by running:
```bash
just setup-intellij
```

Now open the project in IntelliJ. If neede and add modules manually using "Project Structure > Import Module" dialog.

Additionally, please set `scalafmt` as the formatter.

### VSCode setup

If you are using VSCode:
1. Install [Metals](https://scalameta.org/metals/docs/editors/vscode#installation)
2. Open the project in Metals.

Make sure you have `.bsp` directory before you open the project in VSCode.

This might not be enough if your infrastructure is just a part (a module) of your existing Scala project.
For this to work you have to make your build tool aware of the infrastructure code,
for **sbt** create a corresponding module:
   ```scala
lazy val infra = project.in(file("infrastructure")).settings(
   libraryDependencies ++= Seq(
   "org.virtuslab" %% "besom-kubernetes" % "0.1.0", // or any other sdk you are using
   "org.virtuslab" %% "besom-core" % "0.1.0"
   ))
   ```
This just informs your IDE about the existence of the infrastructure module,
DO NOT remove dependencies from `project.scala`, because they are necessary in both places.

## Troubleshooting

### `git` failed to clone or checkout the repository

GitHub might be throttling your requests, try to authenticate:

```bash
export GITHUB_TOKEN=$(gh auth token)
```

## Getting Help

We are sure there are rough edges, and we appreciate you helping out.
If you want to reach out to other folks in the Besom community 
please go to GitHub via [Issues](https://github.com/VirtusLab/besom/issues).
