---
title: Changelog
---

0.2.0
---

* Changed the type of main `Pulumi.run` function from `Context ?=> Output[Exports]` (a [context function](https://docs.scala-lang.org/scala3/reference/contextual/context-functions.html) providing `besom.Context` instance implicitly in it's scope and expecting `Output[Exports]` as returned value) to `Context ?=> Stack`. This change has one core reason: it helps us solve a problem related to dry run functionality that was hindered when a external Output was interwoven in the final for-comprehension. External Outputs (Outputs that depend on real return values from Pulumi engine) no-op their flatMap/map chains in dry run similarly to Option's None (because there is no value to feed to the passed function) and therefore led to exports code not being executed in dry run at all, causing a diff showing that all exports are going to be removed in preview and then recreated in apply phase. New type of `Pulumi.run` function disallows returning of async values - Stack has to be returned unwrapped, synchronously. Stack is just a case class that takes only two arguments: `exports: Exports` and `dependsOn: Vector[Output[?]]`. `exports` serve the same purpose as before and `dependsOn` is used to _use_ all the `Outputs` that have to be evaluated for this stack to be constructed but are not to be exported. You can return a `Stack` that only consists of exports (for instance when everything you depend on is composed into a thing that you export in the final step) using `Stack.export(x = a, y = b)` or a `Stack` that has only dependencies when you don't want to export anything using `Stack(x, y)`. You can also use some resources and export others using `Stack(a, b).export(x = i, y = j)` syntax. Here's an example use of Stack:


```scala
@main def main = Pulumi.run {
  val awsPolicy = aws.iam.Policy("my-policy", ...)
  val s3 = aws.s3.Bucket("my-bucket")
  val logMessage = log.info("Creating your bucket!") // logs are values too!

  Stack(logMessage, awsPolicy).exports(
    url = s3.publicEndpoint
  )
}
```

