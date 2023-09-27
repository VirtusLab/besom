---
title: Tutorial
---

### Introduction
---


Besom, the Pulumi SDK for Scala allows you to create, deploy and manage cloud resources such as databases, serverless
functions and services quickly and safely. In this tutorial your will do exactly that - you will deploy a very basic 
yet functional serverless application built in Scala 3. The target environment will be 
[Amazon Web Services](https://aws.amazon.com/). Everything covered in this tutorial fits into the free tier so the only requirement is to actually have an AWS account. You can [create one here](https://portal.aws.amazon.com/billing/signup) 
if you don't have one.

To start first install all the necessary tools mentioned in [Getting started](./getting_started) section.

You will also need to obtain **AWS Access Key ID** and **AWS Secret Access Key** from IAM Console. Once you you have them,
install [the AWS CLI](https://aws.amazon.com/cli/) for your platform and perform 
```bash
aws configure
```

This will set up your AWS access for use with Pulumi.

:::caution
It's **strongly** recommended that you use an IAM account with **AdministratorAccess** permissions as it guarantees that you won't encounter issues related to missing permissions. Additionally, it's also recommended to set your 
[default region](https://docs.aws.amazon.com/awsconsolehelpdocs/latest/gsg/select-region.html) in AWS Console.
:::

:::tip
After running `aws configure` it's a good idea to run: 
```bash
aws sts get-caller-identity
``` 
This allows you to check that everything is fine! 

If it is you should see a JSON containing your user id, AWS account number and 
your user's ARN.
:::

After all of that is done the last step is to clone the tutorial repository:

```bash
git clone git@github.com:VirtusLab/besom-tutorial.git && cd besom-tutorial
```

Repository contains the sources for AWS Lambda app built with Scala 3. The application itself is already prepared for you,
you don't have to write it from scratch. Moreover, the app is built with GraalVM Native-Image so the build result is a 
native binary allowing for very fast cold starts and miniscule resource use. 

Sources of the app reside in the `./lambda` directory.

You can build the application yourself using the provided `./build.sh` script **if you're running on Linux with AMD64 
processor architecture**. Unfortunately GraalVM does not support compilation of native images for architectures different 
than the one that you are using. For all users on Macs and Windows (and Linux users on ARM platforms) we have provided pre-built artifacts, already packaged into zip files.

These artifacts can be found in `./pre-built/` directory of the repository.

### Architecture
---

The application you are going to deploy to AWS is called CatPost. It's a simple app from simpler times - it's only 
functionality is for the cat lovers to post pictures of their cats along with their names and comments regarding 
the picture.

Here's a chart describing the infrastructure of the application:

![Tutorial app architecture](./assets/tutorial-arch.png)

The app consists of two AWS Lambdas reachable on a public endpoint of Amazon API Gateway. The data about posts is held
in a DynamoDB table and the pictures of cats are stored in a publicly available AWS S3 bucket. Ok, let's deploy!