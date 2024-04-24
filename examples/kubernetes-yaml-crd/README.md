# Kubernetes application using plain YAML files with custom CRD

Example based
on [Kubernetes custom resource definitions](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/).
Uses plain YAML files to create custom CronTab CRD and deploy it to Kubernetes cluster.

## Prerequisites

Follow the steps in [Pulumi Installation and
Setup](https://www.pulumi.com/docs/get-started/install/) and [Configuring Pulumi
Kubernetes](https://www.pulumi.com/docs/intro/cloud-providers/kubernetes/setup/) to get set up with
Pulumi and Kubernetes.

## Running the App

1. Create a new stack:

   ```sh
   $ pulumi stack init
   Enter a stack name: dev
   ```

2. Preview the deployment of the application and perform the deployment:

   ```sh
   $ pulumi up
   ```

3. You can then manage your CronTab objects using kubectl:

   ```sh
   $ kubectl get crontab
   ```
   Should print a list like this:
   ```
   NAME                 AGE
   my-new-cron-object   6s
   ```

4. You can also view the raw YAML data:

   ```sh
   $ kubectl get ct -o yaml
   ```

5. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your
   infrastructure.

6. Once you've finished experimenting, tear down your stack's resources by destroying and removing it:

    ```bash
    $ pulumi destroy --yes
    $ pulumi stack rm --yes
    ```