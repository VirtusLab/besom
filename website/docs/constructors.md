---
title: Resource constructors 
---

## Resources

Resources are the [primary construct of Pulumi](basics.md#resources) programs.

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

Resource constructors always take 3 arguments:

  * resource name - this is an unique name of the resource in Pulumi's state management. Uniqueness is limited
    to stack and the string has to be a `NonEmptyString` (most of the time Besom will be able to infer automatically
    if `String` literal is non-empty but in case of ambiguity you can just add a type ascription `: NonEmptyString`
    or, in case of dynamically obtained values, use `NonEmptyString`'s `apply` method that returns 
    `Option[NonEmptyString]`).

  * resource args - each resource has it's own companion args class (for instance, `aws.s3.Bucket` has a 
    `aws.s3.BucketArgs`) that takes `Input`s of values necessary to configure the resource. Args can be optional
    when there are reasonable defaults and no input is necessary from the user.

  * resource options - [resource options](https://www.pulumi.com/docs/concepts/options/) are additional properties
    that tell Pulumi engine how to apply changes related to this particular resource. Resource options are always 
    optional. These options dictate the order of creation (explicit dependency between resources otherwise unrelated 
    on data level), destruction, the way of performing updates (for instance delete and recreate) and which provider 
    instance to use. There are 3 types of resource options: 
    * `CustomResourceOptions` used with most of the resources defined in provider packages
    * `ComponentResourceOptions` used with resources, both user-defined and remote components (defined in provider packages) 
      and finally
    * `StackReferenceResourceOptions` used with [StackReferences](basics.md/#stack-references). 
    
    To ease the use of resource options a shortcut context function is provided that allows user to summon the constructor 
    of expected resource options type by just typing `opts(...)`. Here's an example: 


```scala
val myAwsProvider: Output[aws.Provider] = aws.Provider("my-aws-provider", aws.ProviderArgs(...))

val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket(
  "my-bucket",
  aws.s3.BucketArgs(...),
  opts(provider = myAwsProvider)
)
```

## Compile time checking

Besom tries to catch as many errors as possible at compile time, examples of our compile time checks are:
- A component resource must register a correct type name with the base constructor in format: `<package>:<module>:<type>`
- A component resource class should have a `(using ComponentBase)` parameter clause at the end of its constructor

