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
```

### Publish local by running:
```bash
sbt publishLocal
```