---
title: Apply methods
---

In Scala `apply` is a well known syntax that turns any class or object into a function and allows user to invoke it. 
Pulumi uses `apply` method in place of functionalities usually covered by `map` and `flatMap` duo in Scala. 
To preserve idiomatic feeling of Besom code Pulumi's `apply` method family is represented by `map` and `flatMap` 
allowing Outputs to be used in for comprehensions.
