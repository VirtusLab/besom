---
sidebar_position: 7
title: Logging
---

In every scope where Pulumi `Context` is available and global Besom import was included user has the capability to summon logger by writing `log` with a following severity level used as a logging method's name, e.g.: `log.warning("Watch out!")`.
