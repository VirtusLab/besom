---
title: Laziness
---


Due to Besom's **lazy semantics** it's possible to declare resources in code and never actually execute that code.

Let's expand on the `s3Bucket` example used in the [Exports](exports.md) section:

```scala
import besom.*
import besom.api.aws

@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
  
  Stack.exports()
}
```
In this case the `s3Bucket` is only declared, but it's not used by the anything in your program:
* it's never _mapped_ or _flatMapped_ to derive another value
* it's not used by `Stack()` or exported using `Stack.export`
* it's never passed as an argument to another Resource

If you were to run `pulumi up` you'd see no resources in the deployment plan. 

There are two ways to avoid this:

**1.** if your infrastructure just depends on the resource being present, and you never use any of the properties that
are the Outputs of that resource you should pass it as an argument to the `Stack` it so that the resource constructor is evaluated:
```scala
import besom.*
import besom.api.aws

@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")

  Stack(s3Bucket) 
}
```
**2.** if your infrastructure or exports mention that resource or any of its properties Besom will implicitly pull 
it into the main flow of the program and evaluate it:
```scala
import besom.*
import besom.api.aws

@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")

  Stack.exports(s3Url = s3Bucket.map(_.websiteEndpoint))
}
```
This will also work if you pass any of the Outputs of any resource as inputs to another resource's constructor.

To help you avoid mistakes with dangling resources that never get evaluated we are working on a feature that will warn 
you during dry run phase that there were resource constructor calls encountered during the evaluation of your program 
that were never composed back into the main flow of the Besom program. 
Meanwhile, it is strongly recommended to enable `-Wunused:all` compiler flag.

There's also one more property of Besom's resource constructors that needs a mention here. Pure, functional programs are 
expected to **execute side effects** as many times as the effect describing them is referenced. 

Let's see this on an example:
```scala
import besom.*
import besom.api.aws

@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
  val launchMissiles: Output[_] = Output { launch() }
  
  Stack(s3Bucket, launchMissiles)
    .export(
      url     = s3Bucket.map(_.websiteEndpoint)    
      fallout = launchMissiles
    )
}
```
If we were to apply that understanding here we would expect that this program would create the S3 bucket twice 
and launch the missiles twice. This isn't true because resource **constructors are [memoized](https://en.wikipedia.org/wiki/Memoization)**. 

Pulumi enforces a strict invariant according to which any given **resource can be declared only once**. 
To deal with it all the calls to a resource constructor with the same arguments are cached and return the same resource 
after the first call. Plain **Outputs on the other hand have no such property** so this program would create the S3 bucket 
once but launch the missiles twice!

To summarise, if you want to avoid surprises with dangling resources you should always compose 
resource constructors into the main flow of your program and avoid using plain Outputs for side effects.