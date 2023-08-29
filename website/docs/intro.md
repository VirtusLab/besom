---
sidebar_position: 1
---

# Intro

Besom is an experimental pulumi-scala implementation.

:::note
Besom - a broom made of twigs tied round a stick.
:::

# Program structure

A besom program should have the following structure:

```scala
import besom.*

// functions used in besom that are outside of `Pulumi.run` have to have `(using Context)` parameter clause
def createAComponent(name: String)(using Context) =
  ...

// `Pulumi.run` <- the main entry point to a besom program
@main
def run = Pulumi.run {
  ...
  val component = createAComponent("Stanley")
  ...

  for
    _ <- component
  yield
    Pulumi.exports(
      aComponentId = component.id
    )
}
```
