---
title: Compiler plugin
---

Besom compiler plugin is a tool that helps to avoid common mistakes when writing Pulumi programs in Scala. 
It is recommended to use it in all Pulumi programs written in Scala.

Currently, the plugin provides the following features:
- It makes it a compile error to try to interpolate Output values in standard Scala string interpolators.

:::info

To use the compiler plugin in `scala-cli`, add the following directive to your build configuration file:

```scala
//> using plugin "org.virtuslab::besom-compiler-plugin:$version"
```


To use the compiler plugin in `sbt`, add the following line to your `build.sbt` file:

```scala
addCompilerPlugin("org.virtuslab" %% "besom-compiler-plugin" % "$version")
```

:::
