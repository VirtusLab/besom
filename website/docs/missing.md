---
title: Missing features
---

Some Pulumi features are not yet implemented. The most notable ones are:
* [Stack References](https://www.pulumi.com/docs/concepts/stack/#stackreferences) are not yet available (targeted for `0.2.0` release).
* [Provider functions](https://www.pulumi.com/docs/concepts/resources/functions/) like `aws.ec2.getAmi` are not yet available (targeted for `0.2.0` release).
* [Component-based packages](https://www.pulumi.com/docs/using-pulumi/pulumi-packages/how-to-author/) a.k.a. "remote components" are not yet available (targeted for `0.2.0` release).
* [Resource aliases](https://www.pulumi.com/docs/concepts/options/aliases/) are not yet available (stretch goal for `0.2.0` release).
* [Automation API](https://www.pulumi.com/docs/guides/automation-api/) is not yet available (targeted for `0.3.0` release).
* [Resource transformations](https://www.pulumi.com/docs/concepts/options/transformations/) are not yet available (targeted for `0.3.0` release).
* [Dynamic Providers](https://www.pulumi.com/docs/concepts/resources/dynamic-providers/) are not yet available.
* [Function serialization](https://www.pulumi.com/docs/concepts/inputs-outputs/function-serialization/) is supported only in Node.js.

Some Pulumi features are **NOT** planned to be implemented:
* [Mock-based unit tests](https://www.pulumi.com/docs/guides/testing/) no planned unless we hear strong community feedback it is needed.
