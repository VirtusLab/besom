# Templates

This folder contains templates for Pulumi projects written in besom.

## Default 
**Dependencies:** random provider 

This template uses Pulumi random API to output a pet name by using [RandomPet](https://www.pulumi.com/registry/packages/random/api-docs/randompet/#random-randompet)

It will set up file structure for you and let you go in any direction you need.

After running `pulumi up` you should see a name of your pet in Outputs of the update.

## Kubernets 
**Dependencies:** kubernetes provider, local kubernetes

This template requiers you to have kubernetes set up locally, you can do this via docker desktop - [how to](https://docs.docker.com/desktop/kubernetes/). 

It deploys nginx to your kubernetes cluster. It's identical to offical Pulumi kuberenetes template for example [the TypeScript one](https://github.com/pulumi/templates/tree/master/kubernetes-typescript).

After running `pulumi up` you can check the deployment using `kubectl get pods` you should see your nginx pod running.