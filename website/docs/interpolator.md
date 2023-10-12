---
title: String interpolation
---

One of the very common use case in Pulumi programs is the need to [interpolate a syntax](https://www.pulumi.com/docs/concepts/inputs-outputs/#convert-input-to-output-through-interpolation). 
Given that the most interesting values arrive asynchronously as Outputs of resources that user creates this usually 
interpolation would require a `map` call or even a `flatMap` + `map` chain for more than one Output (or a for comprehension, alternatively). 

To make this common pattern easier Besom implements a **type-safe interpolator** that works with Outputs seamlessly:

```scala
val o1: Output[Int] = pod.port
val o2: Output[String] = node.hostname
val version: String = "v1"

// or just p"" for shorthand
val http: Output[String]  = pulumi"http://$o2:$o1/api/$version/" 
val https: Output[String] = p"https://$o2:$o1/api/$version/"
```

:::tip

To avoid using standard Scala string interpolators with Outputs by mistake, it is recommended to **use the besom compiler plugin**. 
It will make the compiler fail on any attempt to interpolate an `Output` with a standard string interpolator.

For more information see [Compiler plugin](compiler_plugin.md).
:::

