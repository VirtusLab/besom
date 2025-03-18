## Publish Local

### Prerequisites:
  - [scala-cli](https://scala-cli.virtuslab.org) - enable power mode by running `scala-cli config power true`
  - [sbt](https://www.scala-sbt.org/)
  - [gh](https://cli.github.com) - log in (running `gh auth token` should return a valid github account token)
  - [just](https://github.com/casey/just)
  - [pulumi](https://www.pulumi.com/docs/iac/download-install/)
  - [go](https://go.dev)
  - besom v0.4.0-SNAPSHOT (built locally)
  
### Publish local patches for besom 0.4.0-SNAPSHOT
```bash
just publish-local-aws-mini
just publish-local-besom-json-js
```

### Publish local by running:
```bash
sbt publishLocal
```

### Running examples:
```bash
cd examples/<EXAMPLE_NAME>

#####
# Set up the infrastructure

sbt "clean; compile"
just infra-up -y

######
# Clean up

just infra-down
```

Building the `aws-lambda-graal` example requires setting the following environment variables:
  * `YAGA_AWS_LAMBDA_GRAAL_REMOTE_USER`
  * `YAGA_AWS_LAMBDA_GRAAL_REMOTE_IP`
describing a remote machine on which the native image is supposed to be built.

The remote builder machine has the following requirements:
  * has to run on Linux
  * needs `native-image` command available on `PATH`
  * is recommended to have a lot of memory and threads available (tested with AWS EC2 c7i.4xlarge image)
