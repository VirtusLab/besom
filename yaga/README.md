## Publish Local

### Prerequisites:
  - [scala-cli](https://scala-cli.virtuslab.org) - enable power mode by running `scala-cli config power true`
  - [gh](https://cli.github.com) - log in (running `gh auth token` should return a valid github account token)
  - [just](https://github.com/casey/just)
  - [pulumi](https://www.pulumi.com/docs/iac/download-install/)
  - [go](https://go.dev)
  - besom v0.4.0-SNAPSHOT (built locally)

### Publish local by running:
```bash
cd ..
just publish-local-all
cd yaga
just publish-local-all
```


