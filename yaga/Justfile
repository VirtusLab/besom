besom-core-version-suffix := "0.4"

default:
	@just --list

################################################################################
# Besom AWS lambdas
################################################################################

publish-local-aws-mini aws-version="6.53.0":
  #!/usr/bin/env bash
  export GITHUB_TOKEN=$(gh auth token); \
  cd ..
  just cli packages generate aws:{{aws-version}}
  cd .out/codegen/aws/{{aws-version}}
  scala-cli publish local --project-version {{aws-version}}-mini-core.{{besom-core-version-suffix}}-SNAPSHOT project.scala src/index src/iam src/lambda

################################################################################
# Core
################################################################################

publish-local-model:
  scala-cli publish local model

publish-local-codegen-core:
  scala-cli publish local codegen-core

publish-local-sbt-core:
  #!/usr/bin/env bash
  cd sbt-core
  sbt publishLocal

publish-local-core: publish-local-model publish-local-codegen-core publish-local-sbt-core

################################################################################
# AWS lambdas
################################################################################

publish-local-sdk-aws-lambda: publish-local-model
  scala-cli publish local sdk-aws-lambda

publish-local-sdk-besom-aws-lambda: publish-local-sdk-aws-lambda
  scala-cli publish local sdk-besom-aws-lambda

publish-local-codegen-aws-lambda: publish-local-codegen-core
  scala-cli publish local codegen-aws-lambda

publish-local-sbt-aws-lambda: publish-local-sbt-core
  #!/usr/bin/env bash
  cd sbt-aws-lambda
  sbt publishLocal

publish-local-aws-lambda: publish-local-sdk-aws-lambda publish-local-sdk-besom-aws-lambda publish-local-codegen-aws-lambda publish-local-sbt-aws-lambda

################################################################################
# Aggregated
################################################################################

publish-local-yaga-all: publish-local-core publish-local-aws-lambda

publish-local-all: publish-local-aws-mini publish-local-yaga-all
