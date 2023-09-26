# Templates

This folder contains templates for Pulumi projects written in Besom.

## Default 
**Dependencies:** [random](https://github.com/pulumi/pulumi-random) provider 

To generate random provider run:
```shell
just generate-provider-sdk random 4.13.4
just publish-local-provider-sdk random 4.13.4
```

This template uses Pulumi random API to output a pet name by using [RandomPet](https://www.pulumi.com/registry/packages/random/api-docs/randompet/#random-randompet)

It will set up file structure for you and let you go in any direction you need.

After running `pulumi up` you can check the name of your pet by running `pulumi stack output`.

## Kubernetes 
**Dependencies:** [kubernetes provider](https://github.com/pulumi/pulumi-kubernetes), locally configured kubernetes

To generate kubernetes provider run:
```shell
just generate-provider-sdk kubernetes 4.2.0
just publish-local-provider-sdk kubernetes 4.2.0
```

This template requires you to have kubernetes set up, you can do this locally 
via docker desktop - [how to](https://docs.docker.com/desktop/kubernetes/). If you have already configured `kubectl` 
to point to your external infrastructure (e.g. Google Kubernetes Engine) it will also work.

It deploys nginx to your kubernetes cluster. It's equivalent to the official 
Pulumi kubernetes template for example [the TypeScript one](https://github.com/pulumi/templates/tree/master/kubernetes-typescript).

After running `pulumi up` you can check the deployment using `kubectl get pods` 
you should see your nginx pod running.