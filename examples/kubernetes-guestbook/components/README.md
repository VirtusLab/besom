# Kubernetes Guestbook (Components Variant)

A version of the [Kubernetes Guestbook](https://kubernetes.io/docs/tutorials/stateless-application/guestbook/)
application using Pulumi.

## Running the App

Follow the steps in [Pulumi Installation](https://www.pulumi.com/docs/get-started/install/) and [Kubernetes Setup](https://www.pulumi.com/docs/intro/cloud-providers/kubernetes/setup/) to get Pulumi working with Kubernetes.

Create a new stack:

```sh
$ pulumi stack init
Enter a stack name: testbook
```

This example will attempt to expose the Guestbook application to the Internet with a `Service` of
type `LoadBalancer`. Since minikube does not support `LoadBalancer`, the Guestbook application
already knows to use type `ClusterIP` instead. All you need to do is to tell it whether you're
deploying to use the load balancer or not. 

You can do this by setting the `useLoadBalancer` configuration value to (default is `true`):

```sh
pulumi config set useLoadBalancer <value>
```

Perform the deployment:

```sh
$ pulumi up
...
Updating stack 'testbook'
Performing changes:

     Type                                 Name                Status
 +   pulumi:pulumi:Stack                  guestbook-easy-dev  created (54s)
 +   ├─ besom:example:Application         frontend            created (20s)
 +   │  ├─ kubernetes:core/v1:Namespace   frontend            created (0.33s)
 +   │  ├─ kubernetes:apps/v1:Deployment  frontend            created (5s)
 +   │  └─ kubernetes:core/v1:Service     frontend            created (10s)
 +   └─ besom:example:Redis               cache               created (21s)
 +      ├─ kubernetes:core/v1:Namespace   redis-cache         created (0.26s)
 +      ├─ kubernetes:apps/v1:Deployment  redis-leader        created (7s)
 +      ├─ kubernetes:core/v1:Service     redis-leader        created (10s)
 +      ├─ kubernetes:apps/v1:Deployment  redis-replica       created (9s)
 +      └─ kubernetes:core/v1:Service     redis-replica       created (10s)

Outputs:
    frontend: "http://localhost:80"
    leader  : {
        fqdn: "redis-leader.redis-cache-623162a5.svc.cluster.local"
        url : "redis://redis-leader.redis-cache-623162a5.svc.cluster.local:6379"
    }
    replica : {
        fqdn: "redis-replica.redis-cache-623162a5.svc.cluster.local"
        url : "redis://redis-replica.redis-cache-623162a5.svc.cluster.local:6379"
    }

Resources:
    + 11 created

Duration: 58s
```

And finally - open the application in your browser to see the running application. If you're running
macOS you can simply run:

```sh
open $(pulumi stack output frontend)
```

> _Note_: minikube does not support type `LoadBalancer`; if you are deploying to minikube, make sure
> to run `kubectl port-forward svc/frontend 8080:80` to forward the cluster port to the local
> machine and access the service via `localhost:8080`.

![Guestbook in browser](imgs/guestbook.png)