---
title: Resource constructor asynchronicity
---
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

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