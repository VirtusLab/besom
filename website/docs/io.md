---
title: Inputs and Outputs
---

Outputs are the [primary asynchronous data structure of Pulumi](basics.md#inputs-and-outputs) programs. 

### Outputs

Outputs are:
  * pure and lazy - meaning that they suspend evaluation of code until interpretation, which is perfomed by Besom 
    runtime that runs `Pulumi.run` function at the, so called, end-of-the-world. 

  * monadic - meaning that they expose `map` and `flatMap` operators and can be used in for-comprehensions

Outputs are capable of consuming other effects for which there exists an instance of `ToFuture` typeclass. Besom 
provides 3 such instances: 

- package `besom-core` provides an instance for `scala.concurrent.Future`
- package `besom-cats` provides an instance for `cats.effect.IO`
- package `besom-zio` provides an instance for `zio.Task`

### Inputs

Inputs are Besom types used wherever a value is expected to be provided by user primarily to ease the use of the 
configuration necessary for resource constructors to spawn infrastructure resources. Inputs allow user to provide both 
raw values, values that are wrapped in an `Output`, both of the former or nothing at all when dealing with optional 
fields or even singular raw values or lists of values for fields that expect multiple values.

To make this more digestable - the basic `Input[A]` type is declared as:

```scala
opaque type Input[+A] >: A | Output[A] = A | Output[A]
```

and the `Input.Optional[A]` variant is declared as:

```scala
opaque type Optional[+A] >: Input[A | Option[A]] = Input[A | Option[A]]
```

This allows for things like this:

```scala
val int1: Input[Int] = 23
val int2: Input[Int] = Output(23)
// what if it's an optional value?
val maybeInt1: Input.Optional[Int] = 23
val maybeInt2: Input.Optional[Int] = None
val maybeInt3: Input.Optional[Int] = Some(23)
// yes, but also:
val maybeInt4: Input.Optional[Int] = Output(23)
val maybeInt5: Input.Optional[Int] = Output(Option(23))
val maybeInt6: Input.Optional[Int] = Output(None)
```

This elastic and permissive model was designed to allow a more declarative style and facilitate the implicit
parallelism of evaluation. In fact, Outputs are meant to be thought of as short pipelines that one uses
to transform properties and values obtained from one resource to be used as argument for another. If you're
used to the classic way of working with monadic programs with chains of `flatMap` and `map` or for-comprehensions 
this might seem a bit odd to you - why would we take values wrapped in Outputs as arguments? The answer is: **previews!**

Outputs incorporate semantics of `Option` to support Pulumi's preview / dry-run feature that allows one to see what 
changes will be applied when the program is executed against the actual environment. This, however, means that Outputs
can short-circuit when a computed (provided by the engine) value is missing in dry-run and most properties on resources
belong to this category. It is entirely possible to structure a Besom program the same way one would structure a program
that uses Cats Effect IO or ZIO but once you `flatMap` on an Output value that can be only obtained from actual environment
short-circuiting logic will kick in and all the subsequent `flatMap`/`map` steps will be skipped yielding a broken view
of the changes that will get applied in your next change to the infrastructure. To avoid this problem it is highly 
recommended to write Besom programs in a style highly reminiscent of direct style and use for-comprehensions only to transform 
properties passed from configuration or declared resources to another resources. This way the graph of resources is fully 
known in the dry-run phase and can be properly inspected. Full power of monadic composition should be reserved for situations 
where it is strictly necessary.