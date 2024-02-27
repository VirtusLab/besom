---
title: Introduction
---
<h3>Welcome to Besom, a <a href="https://www.pulumi.com/">Pulumi</a> SDK for Scala 3!</h3>

Pulumi is an [infrastructure-as-code](https://en.wikipedia.org/wiki/Infrastructure_as_code) platform that allows you to 
define any cloud resources your project needs using a programming language of your choice.
Pulumi is a registered trademark of [Pulumi Corporation](https://pulumi.com).

Besom is the implementation of [language SDK for Pulumi](https://www.pulumi.com/docs/languages-sdks/) 
allowing you to use [Scala](https://scala-lang.org/) for all your infrastructure needs.
Both Pulumi and Besom are **free and open source** projects.

:::info
Besom is currently available for evaluation to **early adopters** in the beta stage of the project. We are aligning
our implementation with the rest of Pulumi ecosystem, finding and solving issues and working on general usability,
ergonomics and feature completeness.
:::

### Mission

Besom's mission is to introduce **rich type safety** to the domain of cloud and platform engineering. 
We want to allow our users to benefit from Scala's compiler support to **catch problems and mistakes before the execution** 
of programs that manage the live infrastructure. We believe that **typeful functional** programming is the best approach 
for mission-critical software like infrastructure-as-code solutions.

Besom follows the general model of Pulumi SDKs and operates using the **same basic constructs and primitives**.
It is therefore strongly advised to get acquainted with [**Pulumi's basics**](basics.md) section as
a fast way to get up to speed with the general concepts. 

### Uniqueness

Besom is unique in the fact that it's meant to support all Scala's technological ecosystems transparently:
* Scala's [Future](https://docs.scala-lang.org/overviews/core/futures.html) and [Akka](https://akka.io/) / [Apache Pekko](https://pekko.apache.org/)
* [cats](https://typelevel.org/cats/) and [cats-effect](https://typelevel.org/cats-effect/)
* [ZIO](https://zio.dev/)

To be able to do that, the API has to support **pure, lazy, functional evaluation semantics** as they are the 
lowest common denominator. 
This means that there are some **small differences** in comparison to other Pulumi SDKs that are idiomatic to programs 
written in functional style, to name two: 
- Stack along with its exports are the return value of the main function of the program, 
- smaller chunks of the program have to be composed into the main flow of the program to be executed. 

### Next steps

- [Getting started](getting_started.md) section helps you **get your hands dirty** and figure out the details as you go
- [Basics](basics.md) offers an **executive summary** of Pulumi's key concepts and how they are implemented in Besom
- [Tutorial](tutorial.md) is a **crash course in cloud engineering** using Scala, Pulumi and AWS
- [Architecture Overview](architecture.md) helps you learn more about the **details and differences** between Besom and
  other Pulumi SDKs
