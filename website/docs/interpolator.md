---
sidebar_position: 10
title: Interpolator
---

One of the very common use case in Pulumi programs is the need to interpolate a syntax. Given that the most interesting values arrive asynchronously as Outputs of resources that user creates this usually interpolation would require a `map` call or even a `flatMap` + `map` chain for more than one Output (or a for comprehension, alternatively). To make this common pattern easier Besom implements a type-safe interpolator that works with Outputs seamlessly:

```scala
val o1: Output[Int] = pod.port
val o2: Output[String] = node.hostname
val version: String = "v1"
​
// or just p"" for shorthand
val result: Output[String] = pulumi"http://$o2:$o1/api/$version/" 
```

:::tip

To avoid using standard Scala string interpolators with Outputs by mistake, it is recommended to add use the besom compiler plugin. It will make the compiler to fail on any attempt to interpolate an Output with a standard string interpolator.

For more infornation see [Compiler plugin](compiler_plugin.md).

:::

