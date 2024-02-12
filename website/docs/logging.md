---
title: Logging
---

In every scope where Pulumi `Context` is available and global Besom import was included user has the capability to summon 
logger by writing `log` with a following severity level used as a logging method's name, e.g.: 

```scala
@main def run = Pulumi.run {
  Stack(log.warn("Nothing to do."))
}
```

Logging is an asynchronous, effectful operation and therefore returns an `Output`. This means that all logging statements need to be composed
into other values that will eventually be either passed as `Stack` arguments or exports. This is similar to how logging frameworks for `cats` or `ZIO` behave (eg.: [log4cats](https://github.com/typelevel/log4cats)).

## Why not simply `println`?

Given that you're working with CLI you might be tempted to just `println` some value, but that will have no visible effect.
That's because Besom's Scala code is being executed in a different process than Pulumi. It's Pulumi that drives the 
process by calling Besom. Therefore, you have to use functions provided by Besom for your code to log anything. 
