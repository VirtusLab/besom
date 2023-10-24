# Big idea behind using a Justfile is so that we can have modules like in sbt.

besom-version := `cat version.txt`

language-plugin-output-dir := justfile_directory() + "/.out/language-plugin"
codegen-output-dir := justfile_directory() + "/.out/codegen"
schemas-output-dir := justfile_directory() + "/.out/schemas"
coverage-output-dir := justfile_directory() + "/.out/coverage"
coverage-output-dir-core := coverage-output-dir + "/core"
coverage-output-dir-cats := coverage-output-dir + "/cats"
coverage-output-dir-zio := coverage-output-dir + "/zio"

coverage := "false"

# replace with a function when https://github.com/casey/just/pull/1069 is merged
scala-cli-coverage-options-core := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-core } else { "" }
scala-cli-test-options-core := scala-cli-coverage-options-core

scala-cli-core-dependency-option := "--dep org.virtuslab::besom-core:" + besom-version 

scala-cli-main-options-cats := scala-cli-core-dependency-option
scala-cli-coverage-options-cats := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-cats } else { "" }
scala-cli-test-options-cats := scala-cli-main-options-cats + " " + scala-cli-coverage-options-cats

scala-cli-main-options-zio := scala-cli-core-dependency-option
scala-cli-coverage-options-zio := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-zio } else { "" }
scala-cli-test-options-zio := scala-cli-main-options-zio + " " + scala-cli-coverage-options-zio

publish-maven-auth-options := "--user env:OSSRH_USERNAME --password env:OSSRH_PASSWORD --gpg-key $PGP_KEY_ID --gpg-option --pinentry-mode --gpg-option loopback --gpg-option --passphrase --gpg-option $PGP_PASSWORD"

# This list of available targets
default:
	@just --list

####################
# Aggregate tasks
####################

# Cleans everything
clean-all: clean-sdk clean-out clean-compiler-plugin clean-codegen clean-test-integration clean-test-templates clean-test-examples

# Compiles everything
compile-all: compile-sdk compile-codegen build-language-plugin

####################
# Language SDK
####################

# Compiles the protobufs for the language SDK
compile-pulumi-protobufs:
	scala-cli run --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts/Proto.scala -- all

# Compiles core besom SDK
compile-core:
	scala-cli --power compile core

# Compiles besom cats-effect extension
compile-cats: publish-local-core
	scala-cli --power compile besom-cats {{scala-cli-main-options-cats}}

# Compiles besom zio extension
compile-zio: publish-local-core
	scala-cli --power compile besom-zio {{scala-cli-main-options-zio}}

# Compiles all SDK modules
compile-sdk: publish-local-core compile-cats compile-zio compile-compiler-plugin

# Compiles besom compiler plugin
compile-compiler-plugin:
	scala-cli --power compile compiler-plugin

# Runs tests for core besom SDK
test-core:
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-core}}; fi
	scala-cli --power test core {{ scala-cli-test-options-core }}

# Runs tests for besom cats-effect extension
test-cats: publish-local-core
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-cats}}; fi
	scala-cli --power test besom-cats {{ scala-cli-test-options-cats }}

# Runs tests for besom zio extension
test-zio: publish-local-core
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-zio}}; fi
	scala-cli --power test besom-zio {{ scala-cli-test-options-zio }}

# Runs all tests
test-sdk: compile-sdk test-core test-cats test-zio

# Publishes locally core besom SDK
publish-local-core:
	scala-cli --power publish local core --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes locally besom cats-effect extension
publish-local-cats: publish-local-core
	scala-cli --power publish local besom-cats --project-version {{besom-version}} {{scala-cli-main-options-cats}}  --suppress-experimental-feature-warning

# Publishes locally besom zio extension
publish-local-zio: publish-local-core
	scala-cli --power publish local besom-zio --project-version {{besom-version}} {{scala-cli-main-options-zio}} --suppress-experimental-feature-warning

# Publishes locally all SDK modules: core, cats-effect extension, zio extension
publish-local-sdk: publish-local-core publish-local-cats publish-local-zio

# Publishes locally besom compiler plugin
publish-local-compiler-plugin:
	scala-cli --power publish local compiler-plugin --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes core besom SDK
publish-maven-core:
	scala-cli --power publish core --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom cats-effect extension
publish-maven-cats:
	scala-cli --power publish besom-cats {{ scala-cli-main-options-cats }} --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom zio extension
publish-maven-zio:
	scala-cli --power publish besom-zio {{ scala-cli-main-options-zio }} --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom compiler plugin
publish-maven-compiler-plugin:
	scala-cli --power publish compiler-plugin --project-version {{besom-version}} {{publish-maven-auth-options}}

# Compiles and publishes the previously generated scala API code for the given provider, e.g. `just publish-local-provider-sdk kubernetes`
publish-maven-provider-sdk schema-name schema-version:
	scala-cli --power publish {{codegen-output-dir}}/{{schema-name}}/{{schema-version}} {{publish-maven-auth-options}}

# Cleans core build
clean-core: 
	scala-cli clean core 

# Cleans besom cats-effect extension build
clean-cats:
	scala-cli clean besom-cats 

# Cleans besom ZIO extension build
clean-zio:
	scala-cli clean besom-zio 

# Cleans all SDK builds, sets up all modules for IDE again
clean-sdk: clean-core clean-cats clean-zio clean-compiler-plugin

clean-codegen:
	scala-cli clean codegen

# Cleans besom compiler plugin build
clean-compiler-plugin:
	scala-cli clean compiler-plugin

# Cleans the ./.out directory
clean-out:
	rm -rf ./.out

# Cleans the coverage out directory and sdk compilation output
clean-coverage: clean-sdk
	rm -rf {{coverage-output-dir}}

####################
# Language plugin
####################

# Run go mod tidy for language plugin
tidy-language-plugin:
    cd language-plugin/pulumi-language-scala && \
    go mod tidy

# Builds .jar file with language plugin bootstrap library
build-language-plugin-bootstrap:
	mkdir -p {{language-plugin-output-dir}} && \
	scala-cli --power package language-plugin/bootstrap --assembly -o {{language-plugin-output-dir}}/bootstrap.jar -f

# Builds pulumi-language-scala binary
build-language-host $GOOS="" $GOARCH="":
	mkdir -p {{language-plugin-output-dir}} && \
	cd language-plugin/pulumi-language-scala && \
	go build -o {{language-plugin-output-dir}}/pulumi-language-scala \
	  -ldflags "-X github.com/pulumi/pulumi/sdk/v3/go/common/version.Version={{besom-version}}"

# Builds the entire scala language plugin
build-language-plugin: build-language-plugin-bootstrap build-language-host

# Installs the scala language plugin locally
install-language-plugin: build-language-plugin
	#!/usr/bin/env sh
	just build-language-plugin-bootstrap
	just build-language-host
	output_dir={{language-plugin-output-dir}}/local
	rm -rf $output_dir
	mkdir -p $output_dir
	cp {{language-plugin-output-dir}}/bootstrap.jar $output_dir/
	cp {{language-plugin-output-dir}}/pulumi-language-scala $output_dir/
	pulumi plugin rm language scala -y
	pulumi plugin install language scala {{besom-version}} --file {{language-plugin-output-dir}}/local

# Package the scala language plugin for a given architecture
package-language-plugin $GOOS $GOARCH:
	#!/usr/bin/env sh
	subdir={{ "dist/" + GOOS + "-" + GOARCH }}
	output_dir={{language-plugin-output-dir}}/$subdir
	mkdir -p $output_dir
	just build-language-host $GOOS $GOARCH
	cp {{language-plugin-output-dir}}/bootstrap.jar $output_dir/
	cp {{language-plugin-output-dir}}/pulumi-language-scala $output_dir/
	cd $output_dir
	tar czvf pulumi-language-scala-v{{besom-version}}-{{GOOS}}-{{GOARCH}}.tar.gz *

# Package the scala language plugin for all supported architectures
package-language-plugins-all: build-language-plugin-bootstrap
	just package-language-plugin darwin arm64
	just package-language-plugin darwin amd64
	just package-language-plugin linux arm64
	just package-language-plugin linux amd64
	just package-language-plugin windows arm64
	just package-language-plugin windows amd64

####################
# Codegen
####################

# Compiles codegen module
compile-codegen:
	scala-cli --power compile codegen --suppress-experimental-feature-warning

# Download the schema for a specific provider, e.g. `just get-schema kubernetes`
get-schema schema-name schema-version:
	#!/usr/bin/env sh
	pulumi plugin install resource {{schema-name}} {{schema-version}};
	schema_source={{ if schema-version == "" { schema-name } else { schema-name + "@" + schema-version } }}
	schema_dir="{{schemas-output-dir}}/{{schema-name}}/{{schema-version}}"
	mkdir -p $schema_dir
	pulumi package get-schema $schema_source > $schema_dir/schema.json

# Publishes locally besom codegen
publish-local-codegen:
	scala-cli --power publish local codegen --project-version {{besom-version}} --suppress-experimental-feature-warning

# Generate scala API code for the given provider, e.g. `just generate-provider-sdk kubernetes`
generate-provider-sdk schema-name schema-version:
	scala-cli --power run codegen --suppress-experimental-feature-warning -- {{schemas-output-dir}} {{codegen-output-dir}} {{schema-name}} {{schema-version}} {{besom-version}}

# Compiles the previously generated scala API code for the given provider, e.g. `just compile-provider-sdk kubernetes`
compile-provider-sdk schema-name:
	scala-cli --power compile {{codegen-output-dir}}/{{schema-name}} --suppress-experimental-feature-warning

# Compiles and publishes locally the previously generated scala API code for the given provider, e.g. `just publish-local-provider-sdk kubernetes`
publish-local-provider-sdk schema-name schema-version:
	scala-cli --power publish local {{codegen-output-dir}}/{{schema-name}}/{{schema-version}} --suppress-experimental-feature-warning

####################
# Integration testing
####################

# Runs all integration tests
test-integration: test-integration-core test-integration-compiler-plugin test-integration-codegen test-integration-language-plugin
	scala-cli --power test integration-tests

# Cleans after integration tests
clean-test-integration: clean-test-integration-codegen
	scala-cli --power clean integration-tests

# Runs integration tests for core
test-integration-core: publish-local-codegen publish-local-core install-language-plugin publish-local-compiler-plugin
	just generate-provider-sdk random 4.13.2
	just publish-local-provider-sdk random 4.13.2
	PULUMI_SCALA_PLUGIN_LOCAL_PATH={{language-plugin-output-dir}} \
	scala-cli --power test integration-tests --test-only 'besom.integration.core*'

# Runs integration tests for compiler plugin
test-integration-compiler-plugin: publish-local-codegen publish-local-core install-language-plugin publish-local-compiler-plugin
	scala-cli --power test integration-tests --test-only 'besom.integration.compilerplugin*'

# Runs integration tests for language plugin
test-integration-language-plugin: publish-local-codegen publish-local-core install-language-plugin publish-local-compiler-plugin
	PULUMI_SCALA_PLUGIN_LOCAL_PATH={{language-plugin-output-dir}} \
	scala-cli --power test integration-tests --test-only 'besom.integration.languageplugin*'

# Runs integration tests for codegen
test-integration-codegen: publish-local-codegen
	scala-cli --power test integration-tests --test-only 'besom.integration.codegen*'

# Cleans after the codegen integration tests
clean-test-integration-codegen:
	rm -rf integration-tests/resources/testdata/*/codegen

# Copies test schemas from pulumi repo to the testdata directory
copy-test-schemas:
	scala-cli run --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts/Schemas.scala -- all

####################
# Templates and examples
####################

# Runs a template test
test-template template-name:
	@echo "Testing template {{template-name}}"
	pulumi --color=never --emoji=false new -y --force --dir target/test/{{template-name}} -n templates-test-{{template-name}} --stack templates-test-{{template-name}} ../../../templates/{{template-name}}/
	scala-cli compile target/test/{{template-name}}
	@echo "----------------------------------------"

# Cleans after a template test
clean-test-template template-name:
	@echo "Cleaning template test for {{template-name}}"
	scala-cli clean target/test/{{template-name}} || echo "Could not clean"
	pulumi --color=never --emoji=false stack rm --cwd target/test/{{template-name}} -y || echo "No stack to remove"
	rm -rf ./target/test/{{template-name}} || echo "No directory to remove"
	rm -rf $HOME/.pulumi/stacks/templates-test-{{template-name}} || echo "No directory to remove"
	@echo "----------------------------------------"

# Runs all template tests
test-templates:
	for file in `ls -d templates/*/ | cut -f2 -d'/'`; do just test-template $file; done

# Cleans after template tests
clean-test-templates:
	for file in `ls -d templates/*/ | cut -f2 -d'/'`; do just clean-test-template $file; done

# Runs an example test
test-example example-name:
	@echo "Testing example {{example-name}}"
	scala-cli compile examples/{{example-name}}
	@echo "----------------------------------------"

# Cleans after an example test
clean-test-example example-name:
	@echo "Cleaning example test for {{example-name}}"
	scala-cli clean examples/{{example-name}}
	@echo "----------------------------------------"

# Runs all template tests
test-examples:
	for file in `ls -d examples/*/ | cut -f2 -d'/'`; do just test-example $file; done

# Cleans after template tests
clean-test-examples:
	for file in `ls -d examples/*/ | cut -f2 -d'/'`; do just clean-test-example $file; done

####################
# Website and docs
####################

# Runs tests for website and docs
test-markdown:
	cs launch org.scalameta:mdoc_2.12:2.3.8 -- --in ./README.md ./CONTRIBUTING.md --out target/mdoc-readme --site.version=$(cat version.txt)
	cs launch org.scalameta:mdoc_2.12:2.3.8 -- --in ./website --out target/mdoc-website --exclude node_modules --site.version=$(cat version.txt)

# Cleans after website and docs tests
clean-test-markdown:
	rm -rf target/mdoc-readme
	rm -rf target/mdoc-website

####################
# Demo
####################

# Run the sample kubernetes Pulumi app that resides in ./experimental directory
liftoff:
	#!/usr/bin/env sh
	export PULUMI_CONFIG_PASSPHRASE=""
	cd experimental
	pulumi up --stack liftoff -y

# Reverts the deployment of experimental sample kubernetes Pulumi app from ./experimental directory
destroy-liftoff:
	#!/usr/bin/env sh
	cd experimental
	if (pulumi stack ls | grep liftoff > /dev/null); then
		export PULUMI_CONFIG_PASSPHRASE=""
		pulumi destroy --stack liftoff -y
	fi

# Cleans the deployment of experimental sample kubernetes Pulumi app from ./experimental directory to the ground
clean-liftoff: destroy-liftoff
	#!/usr/bin/env sh
	cd experimental
	if (pulumi stack ls | grep liftoff > /dev/null); then
		pulumi stack rm liftoff -y
	fi
	export PULUMI_CONFIG_PASSPHRASE=""
	pulumi stack init liftoff

# Cleans the deployment of ./experimental app completely, rebuilds core and kubernetes provider SDKs, deploys the app again
clean-slate-liftoff: clean-sdk
	#!/usr/bin/env sh
	just generate-provider-sdk kubernetes 4.2.0 
	just publish-local-core
	just publish-local-compiler-plugin
	just publish-local-provider-sdk kubernetes 4.2.0
	just clean-liftoff
	just liftoff

# Runs 'scala-cli setup-ide' for all modules
setup-intellij:
	for file in `ls */project.scala | cut -f1 -d'/'`; do scala-cli setup-ide $file --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning; done
	for file in `ls */*/project.scala | cut -f1,2 -d'/'`; do scala-cli setup-ide $file --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning; done
	scala-cli setup-ide scripts --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning
	scala-cli setup-ide besom-cats {{scala-cli-core-dependency-option}} --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning
	scala-cli setup-ide besom-zio {{scala-cli-core-dependency-option}} --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning