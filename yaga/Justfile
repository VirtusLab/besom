besom-core-version-suffix := "0.4"

mod example-lambda

publish-local-aws-mini aws-version="6.53.0":
  #!/usr/bin/env bash
  cd ..
  just cli packages generate aws:{{aws-version}}
  cd .out/codegen/aws/{{aws-version}}
  scala-cli publish local --project-version {{aws-version}}-mini-core.{{besom-core-version-suffix}}-SNAPSHOT project.scala src/index src/iam src/lambda


compile-sdk-aws:
  scala-cli compile sdk-aws

publish-local-sdk-aws:
  scala-cli publish local sdk-aws

compile-sdk-besom-aws:
  scala-cli compile sdk-besom-aws

publish-local-sdk-besom-aws:
  scala-cli publish local sdk-besom-aws
