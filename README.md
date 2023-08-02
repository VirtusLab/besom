# Besom
**Besom** - a broom made of twigs tied round a stick. Brooms and besoms are used for protection, to ward off evil spirits, and cleansing of ritual spaces. Also, an experimental pulumi-scala implementation, incidentally.

## Getting started
1. Prerequisites: pulumi, just, java, scala-cli, go
3. Publish locally and install necessary Besom packages:
```bash
just publish-local-sdk
just publish-local-compiler-plugin
just install-language-plugin
```
4. Generate and publish k8s provider:
```bash
just generate-provider-sdk kubernetes
just publish-local-provider-sdk kubernetes
```
3. Write your own infrastructure. You can use the template available in this repository:
```shell
pulumi new ${path_to_the_besom_template_folder}
```
for example:
```shell
cd ..
mkdir infra
cd infra
pulumi new ../besom/template/kubernetes
```
## Explaining the file structure
`Pulumi.yaml` is your main pulumi file, explained [here](https://www.pulumi.com/docs/concepts/projects/project-file/). 

`project.scala` is the file containing your dependencies.

`Main.scala` is the entrypoint for your infrastructure as code. Resources created in `Pulumi.run{ ... }` block will be created by pulumi.

## Setting up the code editor

If you are using IntelliJ: 
1. install scala plugin
2. use bsp - [how to](https://www.jetbrains.com/help/idea/bsp-support.html)  

If you are using VSCode:
1. install Metals
2. open folder with your infrastructure and start Metals.

This might not be enough if infrastructure is just a part (a module) of your existing scala project. For this to work you have to make your build tool aware of infrastructure code, for **sbt** create a corresponding module: 
   ```scala
lazy val infra = project.in(file("infrastructure")).settings(
   libraryDependencies ++= Seq(
   "org.virtuslab" %% "besom-kubernetes" % "0.0.1-SNAPSHOT",
   "org.virtuslab" %% "besom-core" % "0.0.1-SNAPSHOT"
   ))
   ```
This just informs your IDE about the existence of infrastructure module Do not remove dependencies from `project.scala` they are necessary in both places.

## Tips
- Pass `Context` everywhere you are using pulumi, for example when you are creating a resource.
- Make sure your code is called by pulumi. Either by referencing your resources in some other ones or having their Output monad merged with others. 
- Use whatever scala concepts you are familiar with, infrastructure as code in Besom is still a scala program, so you have the full potential of the language to work with.
- Pay attention to the types. You will be instantiating case classes to pass parameters, note their package of origin.