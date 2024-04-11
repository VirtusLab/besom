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

:::info
A good observer will notice that all these computations started in a fire-and-forget fashion have to be awaited somehow 
and that is true. Besom does await for all spawned Outputs to be resolved before finishing the run via a built-in task 
tracker passed around in `Context`.
:::

There is an explicit way to inform Pulumi engine that some of the resources have to be created, updated or 
deleted sequentially. To do that, one has to pass [resource options](https://www.pulumi.com/docs/concepts/options/)
to adequate resource constructors with `dependsOn` property set to resource to await for. Here's an example:

```scala
for 
  a <- Bucket("first")
  b <- Bucket("second", BucketArgs(), opts(dependsOn = a))
yield ()
```

There's also `deletedWith` property that allows one to declare that some resources will get cleaned up when another
resource is deleted and that trying to delete them *after* that resource is deleted will fail. A good example of such
relationship might be Kubernetes, where deletion of a namespace takes down all resources that belong do that namespace.

This manual wiring is only necessary for cases when there are no data dependencies between defined resources. In a case 
like this:

```scala
  val defaultMetadata = k8s.meta.v1.inputs.ObjectMetaArgs(
    labels = Map("app" -> "my-app")
  )

  val deployment = k8s.apps.v1.Deployment(
    name = "my-app-deployment",
    k8s.apps.v1.DeploymentArgs(
      metadata = defaultMetadata,
      spec = k8s.apps.v1.inputs.DeploymentSpecArgs(
        // removed for brevity
      )
    )
  )

  val service = k8s.core.v1.Service(
    name = "my-app-service",
    k8s.core.v1.ServiceArgs(
      spec = k8s.core.v1.inputs.ServiceSpecArgs(
        selector = appLabels,
        // removed for brevity
      ),
      metadata = defaultMetadata
    ),
    opts(dependsOn = deployment)
  )
```

there is no data dependency between kubernetes deployment and kubernetes service because kubernetes links these
entities using labels. There's a guarantee that service points towards the correct deployment because a programming 
language is being used and that allows to define a common constant value that is reused to define them. There is,
however, no output property of Deployment used in definition of Service and therefore Pulumi engine can't infer
that it should actually wait with the creation of Service until Deployment is up. In such cases one can use 
`dependsOn` property to inform the engine about such a relationship between resources. 
