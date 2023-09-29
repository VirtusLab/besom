---
title: Introduction
---
<h3>Welcome to Besom, a <a href="https://www.pulumi.com/">Pulumi</a> SDK for Scala 3!</h3>

Pulumi is an infrastructure-as-code platform that allows you to define any cloud resources your project needs using 
a programming language of your choice. Besom is the implementation of language SDK for Pulumi allowing you to use Scala 
for all your infrastructure needs.

Besom's mission is to introduce rich type safety to the domain of cloud and platform engineering. 
We want to allow our users to benefit from Scala's compiler support to catch problems and mistakes before the execution 
of programs that manage the live infrastructure. We believe that typeful functional programming is the best approach for 
mission-critical software like infrastructure-as-code solutions.

:::info
​Besom is currently available for evaluation to early adopters in the beta stage of the project. We are aligning our 
implementation with the rest of Pulumi ecosystem, finding and solving issues and working on general usability, ergonomics 
and feature completeness. 
:::
​
Besom follows the general model of Pulumi SDKs and operates using the same basic constructs and primitives. 
It is therefore strongly advised to get acquainted with Pulumi's [concepts](https://www.pulumi.com/docs/concepts/) documentation as all of that information 
applies to Besom also. 

Besom is unique in the fact that it's meant to support all Scala's technological ecosystems transparently:
​
* Scala's Future and Akka / Pekko
* cats and cats-effect
* ZIO
 
​
To be able to do that, the API has to support pure, lazy, functional evaluation semantics as they are the lowest common denominator. 
This means that there are some small differences in comparison to other Pulumi SDKs that are idiomatic to programs 
written in functional style (e.g.: Stack exports are the return value of the main function of the program, 
smaller chunks of the program have to be composed into the main flow of the program to be executed). 
To learn more about the semantics of Besom we invite you to the [Architecture](./architecture.md) page where this topic is explored in detail.

If you are more keen to just get your hands dirty and figure out the details as you go - next step should be the 
[Getting started](./getting_started.md) section.
​
