---
title: Exports
---

Exports a.k.a. [Stack Outputs](https://www.pulumi.com/learn/building-with-pulumi/stack-outputs/) 
are a way to share values between stacks.

In other SDKs you are free to call an `export` method on the Pulumi Context object whenever you want in a program. 
Besom's functional design disallows this - since your program is a function exported keys and values have to be 
the last value your main function returns, e.g.:

```scala
import besom.*
import besom.api.aws

@main def run = Pulumi.run {
  for
    bucket <- aws.s3.Bucket("my-bucket", ...) 
  yield Pulumi.exports(
    bucketUrl = bucket.websiteEndpoint
  )
}
```
