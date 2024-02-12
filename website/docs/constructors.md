---
title: Resource constructors and asynchronicity
---
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Resources

Resources are the [primary construct of Pulumi](basics.md#resources) programs.

## Outputs

Outputs are the [primary asynchronous data structure of Pulumi](basics.md#inputs-and-outputs) programs.

## Resource constructor syntax

Most Pulumi SDKs expect you to create resource objects by invoking their constructors with `new` keyword, 
for example in TypeScript: 

```typescript
const s3Bucket: aws.s3.Bucket = new aws.s3.Bucket("my-bucket")
```

This operation returns a resource object of type `aws.s3.Bucket` that has several fields, 
each of an [`Output[A]`](#outputs) type. The values will be provided by the engine later, when the resource is created
and its properties can be fetched.

For all of that to happen this synchronous constructor call has to **hide the complex, asynchronous machinery** that 
triggers the communication with Pulumi engine and resolves underlying `Promise`-like datatype wrapped by each Output. 
This cannot be done in a pure way and due to that **resource constructors in Besom also return Outputs** lifting them 
to become primary construct in terms of which user declares his program. 

This is very similar to the known pattern involving Scala's Future's, cats `IO` or `ZIO` where once you start 
expressing your program using such a construct you build everything in these terms and only `Await` or `unsafeRun` 
them in your main function. 

Here's an example of said resource constructor in Besom:

```scala
val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
```

:::tip
We have retained the CamelCase naming convention of resource constructors for parity with other Pulumi SDKs. 
You can always expect resource constructor names to start with capital letter. 
:::

## Resource asynchronicity

Resources in Besom have an interesting property related to the fact that Pulumi's runtime is asynchronous. 
One could suspect that in following snippet resources are created sequentially due to monadic syntax:

```scala
for 
  a <- aws.s3.Bucket("first")
  b <- aws.s3.Bucket("second")
yield ()
```

This isn't true. Pulumi expects that a language SDK will declare resources as fast as possible. Due to this
fact resource constructors return immediately after they spawn a Resource object. A resource object is just a 
plain case class with each property expressed in terms of Outputs. The work necessary for resolution of these
properties is executed asynchronously. In the example above both buckets will be created in parallel. 

Given that a piece of code is worth more than a 1000 words, below you can find code snippets that explain these 
semantics using known Scala technologies. In each of them `Output` is replaced with a respective async datatype
to explain what internals of Besom are actually doing when resource constructors are called (oversimplified a 
bit).

<Tabs>
  <TabItem value="Future" label="stdlib Future" default>

```scala
// internal function, here just to represent types
def resolveResourceAsync(name: String, args: Args, promises: Promise[_]*): Future[Unit] = ???

// resource definition
case class Bucket(bucket: Future[String])
object Bucket:
  def apply(name: String, args: BucketArgs = BucketArgs()): Future[Bucket] = 
    // create a promise for bucket property
    val bucketNamePromise = Promise[String]() 
    // kicks off async resolution of the resource properties
    resolveResourceAsync(name, args, bucketNamePromise) 
    // returns immediately
    Future.successful(Bucket(bucketNamePromise.future)) 

// this just returns a Future[Unit] that will be resolved immediately
for 
  a <- Bucket("first")
  b <- Bucket("second")
yield ()
```

  </TabItem>
  <TabItem value="ce" label="Cats Effect IO">

```scala
// internal function, here just to represent types
def resolveResourceAsync(name: String, args: Args, promises: Deferred[IO, _]*): IO[Unit] = ???

// resource definition
case class Bucket(bucket: IO[String])
object Bucket:
  def apply(name: String, args: BucketArgs = BucketArgs()): IO[Bucket] = 
    for 
      // create a deferred for bucket property
      bucketNameDeferred <- Deferred[IO, String]() 
      // kicks off async resolution of the resource properties
      _ <- resolveResourceAsync(name, args, bucketNameDeferred).start 
    yield Bucket(bucketNameDeferred.get) // returns immediately

// this just returns a IO[Unit] that will be resolved immediately
for 
  a <- Bucket("first")
  b <- Bucket("second")
yield ()
```

  </TabItem>
  <TabItem value="zio" label="ZIO">

```scala
// internal function, here just to represent types
def resolveResourceAsync(name: String, args: Args, promises: Promise[_]*): Task[Unit] = ???

// resource definition
case class Bucket(bucket: Task[String])
object Bucket:
  def apply(name: String, args: BucketArgs = BucketArgs()): Task[Bucket] = 
    for 
      // create a promise for bucket property
      bucketNamePromise <- Promise.make[Exception, String]() 
      // kicks off async resolution of the resource properties
      _ <- resolveResourceAsync(name, args, bucketNameDeferred).fork 
    yield Bucket(bucketNameDeferred.await) // returns immediately

// this just returns a Task[Unit] that will be resolved immediately
for 
  a <- Bucket("first")
  b <- Bucket("second")
yield ()
```

  </TabItem>
</Tabs>

There is a way to inform Pulumi engine that some of the resources have to be created, updated or deleted 
sequentially. To do that, one has to pass [resource options](https://www.pulumi.com/docs/concepts/options/)
to adequate resource constructors with `dependsOn` property set to resource to await for. Here's an example:
```scala
for 
  a <- Bucket("first")
  b <- Bucket("second", BucketArgs(), opts(dependsOn = a))
yield ()
```


:::info
A good observer will notice that all these forks have to be awaited somehow and that is true. Besom
does await for all spawned Outputs to be resolved before finishing the run.
:::

## Compile time checking

Besom tries to catch as many errors as possible at compile time, examples of our compile time checks are:
- A component resource must register a correct type name with the base constructor in format: `<package>:<module>:<type>`
- A component resource class should have a `(using ComponentBase)` parameter clause at the end of its constructor

