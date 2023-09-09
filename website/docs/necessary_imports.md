---
sidebar_position: 5
title: Necessary imports
---

Each Besom program file should have an `import besom.*` clause to bring all the user-facing types and functions into scope. If using a Cats-Effect or ZIO variant this import is respectively `import besom.cats.*` or `import besom.zio.*`.
