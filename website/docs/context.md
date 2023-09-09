---
sidebar_position: 6
title: Context
---

Pulumi's Context is passed around implicitly via Scala's [Context Function] and only used by user-facing functions. The `Pulumi.run` block exposes `Context` implicitly. All functions that belong to Besom program but are defined outside of the `Pulumi.run` block should have the following `using` clause: `(using Context)` (or `besom.Context` using a fully qualified name of the type).
