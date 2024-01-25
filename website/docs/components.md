---
title: Components
---

Pulumi allows users to define their own logical aggregations of resources called [components](https://www.pulumi.com/docs/concepts/resources/components/) and so does Besom. 
The API is a **bit different** in [comparison to other Pulumi SDKs](https://www.pulumi.com/docs/concepts/resources/components/). 
User is expected to define a `case class` that will contain 
all the `Output` properties one wants to expose from the internals of the component. 

The `case class` has to:
1. extend `besom.ComponentResource` trait
2. have a `(using ComponentBase)` clause after it's primary constructor
3. have a `derives RegistersOutputs` at the end of it's class declaration

​
Next step is to declare a function that will use the `component` constructor function to compose all the resources together. 
Every `component` expects:
- a **unique resource name** as the first argument (usually it's an information that the end user has to provide)
- a [**resource type** which is a string formatted specifically to fit Pulumi's model](https://www.pulumi.com/docs/concepts/resources/names/#types)
- and, as the second argument list of `component` function, the **block of code** that defines all the resources 
that should belong to the component.
​

Here's a small example using the S3 bucket resource again:
```scala
import besom.*
import besom.api.aws
import java.time.OffsetDateTime

case class ZooVisit(
  catPicsUrl: Output[String], 
  parrotPicsUrl: Output[String]
)(using ComponentBase) 
  extends ComponentResource 
  derives RegistersOutputs

def ZooVisit(date: OffsetDateTime)(using Context): Output[ZooVisit] = 
  component(s"zoo-visit-at-$date", "user:component:ZooVisit") { 
    for 
      catsUrl    <- aws.s3.Bucket(s"cats-$date").websiteEndpoint
      parrotsUrl <- aws.s3.Bucket(s"parrot-$date").websiteEndpoint
    yield ZooVisit(catsUrl, parrotsUrl)
  }

@main def main = Pulumi.run {
  val visit = ZooVisit(OffsetDateTime.now())

  Stack.exports(
    cats    = visit.map(_.catPicsUrl),
    parrots = visit.map(_.parrotPicsUrl)
  )
}
```
