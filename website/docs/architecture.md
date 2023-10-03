---
title: Overview
---


Pulumi runtime is **asynchronous by design**. The goal is to allow the user's program to declare all the necessary resources 
as fast as possible so that Pulumi engine can make informed decisions about which parts of the deployment plan can be 
executed in parallel and therefore yield good performance. 

Each of the Pulumi SDKs reflects this reality by leveraging the language's asynchronous datatype to implement 
the internals of the SDK that communicate with Pulumi engine via gRPC. 
For Python, it's `asyncio`, for JavaScript and TypeScript it's `Promise`, 
for C# it's `Task` and for Java it's `CompletableFuture`. 

Scala is a **bit different** in this regard. Due to extraordinary amount of innovation happening in the community and the 
overall focus on concurrency and asynchronicity Scala now has 3 main asynchronous, concurrent data types:
- standard library's `Future`, which is used heavily in Akka / Pekko ecosystems, 
- cats-effect `IO` used extensively by the cats ecosystem 
- and `ZIO` that also has its own ecosystem. 

The last two of these data types are [lazy evaluated](https://en.wikipedia.org/wiki/Lazy_evaluation). 

To support and integrate them with Besom a decision was made to encode the SDK using the same **lazy and pure semantics** 
of execution that leverage the preferred datatype of the user. While this architectural choice has little impact on what 
can be done currently in standalone Pulumi programs, in the future we are going to support Pulumi's Automation API 
which allows users to directly embed Besom into their applications. 
It is at that point when direct integration with all 3 technological stacks will be the most meaningful.
​

Besom stands alone in this choice and due to it **has some differences** in comparison to how other Pulumi SDKs operate. 

Following sections explain and showcase said differences:
- [Resource constructors](constructors) - resource constructors are pure functions that return Outputs
- [Context](context) - context is passed around implicitly via Scala's Context Function
- [Exports](exports) - your program is a function that returns Stack Outputs
- [Laziness](laziness) - dangling resources are possible and resource constructors are memoized
- [Apply method](apply_methods) - use `map` and `flatMap` to compose Outputs, not `apply`
- [Logging](logging) - all logging statements need to be composed into the main flow
- [Lifting](lifting) - first class support of lifting via Scala 3 extension methods
- [String interpolation](interpolation) - use `p` type-safe string interpolator
- [Components](components) - use `case class` to define components
- [Compiler plugin](compiler_plugin) - use the compiler plugin to catch mistakes early and avoid common pitfalls
