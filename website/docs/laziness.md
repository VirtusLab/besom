---
title: Laziness
---


Due to Besom's lazy semantics it's possible to declare resources in code and never actually execute that code. 
Let's expand on the `s3Bucket` example above:
​
```scala
import besom.*
import besom.api.aws
​
@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
  
  Output(Pulumi.exports())
}
```
In this case the `s3Bucket` is only declared, but it's not composed into the main flow of the program 
(it's never _mapped_/_flatMapped_) so if you were to run `pulumi up` you'd see no resources in the deployment plan. 
There are two ways to avoid this:

**1.** if your infrastructure just depends on the resource being present, and you never use any of the properties that
are the Outputs of that resource you should manually compose it so that the resource constructor is evaluated:
```scala
import besom.*
import besom.api.aws
​
@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
​
  for 
    _ <- s3Bucket
  yield Pulumi.exports() 
  
  // or just s3Bucket.map(_ => Pulumi.exports())
}
```
**2.** if your infrastructure or exports mention that resource or any of its properties Besom will implicitly pull 
it into the main flow of the program and evaluate it:
```scala
import besom.*
import besom.api.aws
​
@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
​
  Output(Pulumi.exports(s3Url = s3Bucket.map(_.websiteEndpoint)))
}
```
This will also work if you pass any of the Outputs of any resource as inputs to another resource's constructor.
​

To help you avoid mistakes with dangling resources that never get evaluated we are working on a feature that will warn 
you during dry run phase that there were resource constructor calls encountered during the evaluation of your program that were never composed back into the main flow of the Besom program.
​

There's also one more property of Besom's resource constructors that needs a mention here. Pure, functional programs are 
expected to execute side effects as many times as they are executed. Let's see this on an example:
```scala
import besom.*
import besom.api.aws
​
@main def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")
  val launchMissiles: Output[_] = Output { launch() }
  
  for 
    _ <- s3Bucket
    _ <- s3Bucket    
    _ <- launchMissiles
    _ <- launchMissiles
  yield Pulumi.exports() 
}
```
If we were to apply that understanding here we would expect that this program would create the S3 bucket twice 
and launch the missiles twice. This isn't true because resource **constructors are [memoized](https://en.wikipedia.org/wiki/Memoization)**. 
Pulumi enforces a strict invariant according to which any given resource can be declared only once. 
To deal with it all the calls to a resource constructor with the same arguments are cached and return the same resource 
after the first call. Plain Outputs on the other hand have no such property so this program would create the S3 bucket 
once but launch the missiles twice!