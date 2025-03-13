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
