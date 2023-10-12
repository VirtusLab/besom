---
title: Context and imports
---

Pulumi's Context is passed around implicitly via Scala's [Context Function](https://docs.scala-lang.org/scala3/reference/contextual/context-functions.html) and only used by user-facing functions. 
The `Pulumi.run` block exposes `Context` implicitly. All functions that belong to Besom program but are defined outside 
the `Pulumi.run` block should have the following `using` clause: `(using Context)` or `(using besom.Context)` using a fully qualified name of the type.

Each Besom program file should have an `import besom.*` clause to bring all the user-facing types and functions into scope. 
If using a Cats-Effect or ZIO variant this import is respectively `import besom.cats.*` or `import besom.zio.*`.

:::tip
Please pay attention to your dependencies, **only use `org.virtuslab::besom-*`** and not `com.pulumi:*`.<br/>
Besom **does NOT depend on Pulumi Java SDK**, it is a completely separate implementation.
:::

Here's a sample:

```scala
import besom.*

// functions used in besom that are outside of `Pulumi.run` 
// have to have `(using Context)` parameter clause
def createAComponent(name: String)(using Context) =
...

// `Pulumi.run` <- the main entry point to a besom program
@main def run = Pulumi.run {
  ...
  val component = createAComponent("Stanley")
  ...

  for
    _ <- component
  yield Pulumi.exports(
    aComponentUrn = component.urn
  )
}
```