---
title: Resource constructors, outputs and asychronicity
---

Most Pulumi SDKs expect you to create resource objects by invoking their constructors with `new` keyword, for example in TypeScript: 

```typescript
const s3Bucket: aws.s3.Bucket = new aws.s3.Bucket("my-bucket")
```

This operation returns a resource object of type `aws.s3.Bucket` that has several fields, each of an `Output[A]` type. Outputs are the primary asynchronous data structure in Pulumi and they signify values that will be provided by the engine later, when the resource is created and it's properties can be fetched. 

For all of that to happen this synchronous constructor call has to hide the complex, asynchronous machinery that triggers the communication with Pulumi engine and resolves underlying `Promise`-like datatype wrapped by each Output. This cannot be done in a pure way and due to that resource constructors in Besom also return Outputs lifting them to become primary construct in terms of which user declares his program. 

This is very similar to the known pattern involving Scala's Future's, cats `IO` or `ZIO` where once you start expressing your program using such a construct you build everything in these terms and only `Await` or `unsafeRun` them in your main function. 

Here's an example of said resource constructor in Besom:

```scala
val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
```

:::tip
We have retained the CamelCase naming convention of resource constructors for parity with other Pulumi SDKs. 
You can always expect resource constructor names to start with capital letter. 
:::
