besom-core-version-suffix := "0.4"
besom-version := "0.4.0-SNAPSHOT"

default:
	@just --list


################################################################################
# Besom Json JS
################################################################################

publish-local-besom-json-js:
  #!/usr/bin/env bash
  cd ..
  scala-cli publish local besom-json --project-version {{besom-version}} --js


################################################################################
# Besom AWS lambdas
################################################################################

publish-local-aws-mini aws-version="6.70.0":
  #!/usr/bin/env bash
  export GITHUB_TOKEN=$(gh auth token); \
  cd ..
  just cli packages generate aws:{{aws-version}}
  cd .out/codegen/aws/{{aws-version}}
  scala-cli publish local --project-version {{aws-version}}-mini-core.{{besom-core-version-suffix}}-SNAPSHOT project.scala src/index src/iam src/lambda
