---
sidebar_position: 1
title: Introduction
---

Welcome to Besom, a [Pulumi](https://www.pulumi.com/) SDK for Scala 3!
​
Besom is currently available for evaluation to early adopters in beta stage of the project. We are aligning our implementation with the rest of Pulumi ecosystem, finding and solving issues and working on general usability, ergonomics and feature completeness. 
​
Besom follows the general model of Pulumi SDKs and operates using the same basic constructs and primitives. It is therefore strongly advised to get acquainted with Pulumi's [concepts](https://www.pulumi.com/docs/concepts/) documentation as all of that information applies to Besom also. Having said that, Besom is unique in the fact that it's meant to support all Scala's technological ecosystems:
​
* scala's Future and Akka / Pekko
* cats and cats-effect
* ZIO
 
​
To be able to do that, the API has to support pure, lazy, functional evaluation semantics as they are the lowest common denominator*. This means that there are some relatively small differences in comparison to other Pulumi SDKs that are idiomatic to programs written in functional style (e.g.: Stack exports are the return value of the main function of the program, smaller chunks of the program have to be composed into the main flow of the program to be executed). To learn more about the semantics of Besom we invite you to the [Architecture](./architecture.md) page where this topic is explored in detail.
​
Besom's mission is to introduce rich type safety to the domain of cloud and platform engineering. We want to allow our users to benefit from Scala's compiler support to catch problems and mistakes before the execution of programs that manage the live infrastructure. We believe that typeful functional programming is the best approach for mission critical software like infrastructure-as-code solutions.
​
​
*It is possible to turn a lazy program into a strict one by forcing evaluation but it's impossible to go the other way around and turn a strict program into a lazy one.
