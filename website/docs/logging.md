---
title: Logging
---

In every scope where Pulumi `Context` is available and global Besom import was included user has the capability to summon logger by writing `log` with a following severity level used as a logging method's name, e.g.: 

```scala
@main def run = Pulumi.run {
  for 
    _ <- log.warning("Nothing to do.")
  yield Pulumi.exports()
}
```

Logging is an asynchronous operation and returns an `Output`. This means that all logging statements need to be composed
into the flow of your program. This is similar to how logging frameworks for `cats` or `ZIO` behave (eg.: [log4cats](https://github.com/typelevel/log4cats)).
