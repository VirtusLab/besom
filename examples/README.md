# Besom Examples

This repository contains examples of using Besom 
to build and deploy cloud applications and infrastructure.

Each example has a two-part prefix, `<cloud>-<topic>`, to indicate which `<cloud>` and `<topic>` it pertains to.
For example, `<cloud>` could be `aws` for [Amazon Web Services](https://github.com/pulumi/pulumi-aws), 
`azure` for [Microsoft Azure](https://github.com/pulumi/pulumi-azure), 
`gcp` for [Google Cloud Platform](https://github.com/pulumi/pulumi-gcp), 
`kubernetes` for [Kubernetes](https://github.com/pulumi/pulumi-kubernetes).

See the [Besom documentation](https://virtuslab.github.io/besom/docs/intro/) for more
details on getting started with Besom.

## Checking Out a Single Example

You can checkout selected examples you want by using a [sparse checkout](https://git-scm.com/docs/git-sparse-checkout). 
The following example shows how checkout only the example you want.

```bash
$ mkdir examples && cd examples
$ git init
$ git remote add origin -f https://github.com/VirtusLab/besom
$ git config core.sparseCheckout true
$ echo examples/<example> >> .git/info/sparse-checkout
$ git pull origin master
```
