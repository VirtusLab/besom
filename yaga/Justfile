besom-core-version-suffix := "0.4"

publish-local-aws-mini aws-version="6.53.0":
  #!/usr/bin/env bash
  export GITHUB_TOKEN=$(gh auth token); \
  cd ..
  just cli packages generate aws:{{aws-version}}
  cd .out/codegen/aws/{{aws-version}}
  scala-cli publish local --project-version {{aws-version}}-mini-core.{{besom-core-version-suffix}}-SNAPSHOT project.scala src/index src/iam src/lambda

compile-model:
  scala-cli compile model

publish-local-model:
  scala-cli publish local model

compile-codegen:
  scala-cli compile codegen

publish-local-codegen:
  scala-cli publish local codegen

compile-sdk-aws:
  scala-cli compile sdk-aws

publish-local-sdk-aws: publish-local-model
  scala-cli publish local sdk-aws

compile-sdk-besom-aws:
  scala-cli compile sdk-besom-aws

publish-local-sdk-besom-aws: publish-local-sdk-aws
  scala-cli publish local sdk-besom-aws

compile-codegen-aws:
  scala-cli compile codegen-aws

publish-local-codegen-aws: publish-local-codegen
  scala-cli publish local codegen-aws

publish-local-sbt-aws-lambda:
  #!/usr/bin/env bash
  cd sbt-plugin
  sbt publishLocal

publish-local-all: publish-local-aws-mini publish-local-model publish-local-codegen publish-local-sdk-aws publish-local-sdk-besom-aws publish-local-codegen-aws publish-local-sbt-aws-lambda