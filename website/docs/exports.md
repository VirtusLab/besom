---
title: Exports
---

Pulumi stack can export values as [Stack Outputs](basics.md#stack-outputs)
to expose values to the user and share values between stacks using [Stack References](basics.md#stack-references).

In other SDKs you are free to call an `export` method on the Pulumi Context object whenever you want in a program. 
Besom's functional design disallows this - since **your program is a function**, exported keys and values have to be a part of the final Stack value that your main function returns. 

To export outputs from your stack use `Stack.exports`, e.g.:

```scala
import besom.*
import besom.api.aws

@main def run = Pulumi.run {
  val bucket = aws.s3.Bucket("my-bucket") 

  Stack.exports(
    bucketUrl = bucket.websiteEndpoint
  )
}
```

This will export a key `bucketUrl` with the value of the `websiteEndpoint` property of the `bucket` resource.
