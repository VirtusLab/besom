# Big idea behind using a Justfile is so that we can have modules like in sbt.

publish-version := "0.0.1-SNAPSHOT"
language-plugin-output-dir := justfile_directory() + "/.out/language-plugin"
codegen-output-dir := justfile_directory() + "/.out/codegen"
schemas-output-dir := justfile_directory() + "/.out/schemas"
coverage-output-dir := justfile_directory() + "/.out/coverage"
coverage-output-dir-core := coverage-output-dir + "/core"
coverage-output-dir-cats := coverage-output-dir + "/cats"
coverage-output-dir-zio := coverage-output-dir + "/zio"
coverage-reports-dir := coverage-output-dir + "/reports"

coverage := "false"

# replace with a function when https://github.com/casey/just/pull/1069 is merged
scala-options-core := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-core } else { "" }
scala-options-cats := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-cats } else { "" }
scala-options-zio := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-zio } else { "" }

# This list of available targets
default:
    @just --list

####################
# Aggregate tasks
####################

# Cleans everything
clean-all: clean-sdk clean-out clean-compiler-plugin

# Compiles everything
compile-all: compile-sdk compile-codegen build-language-plugin

####################
# Language SDK
####################

# Compiles core besom SDK
compile-core:
	scala-cli compile core -v -v -v

# Compiles besom cats-effect extension
compile-cats:
	scala-cli compile besom-cats

# Compiles besom zio extension
compile-zio:
	scala-cli compile besom-zio

# Compiles all SDK modules
compile-sdk: publish-local-core compile-cats compile-zio compile-compiler-plugin

# Compiles besom compiler plugin
compile-compiler-plugin:
	scala-cli compile compiler-plugin

# Runs tests for core besom SDK
test-core:
	@if [[ {{ coverage }} == "true" ]]; then mkdir -p {{coverage-output-dir-core}}; fi
	scala-cli test core {{ scala-options-core }}

# Runs tests for besom cats-effect extension
test-cats:
	@if [[ {{ coverage }} == "true" ]]; then mkdir -p {{coverage-output-dir-cats}}; fi
	scala-cli test besom-cats {{ scala-options-cats }}

# Runs tests for besom zio extension
test-zio:
	@if [[ {{ coverage }} == "true" ]]; then mkdir -p {{coverage-output-dir-zio}}; fi
	scala-cli test besom-zio {{ scala-options-zio }}

# Runs all tests
test-sdk: compile-sdk test-core test-cats test-zio

# Publishes locally core besom SDK
publish-local-core:
  scala-cli --power publish local core --project-version {{publish-version}} --doc=false

# Publishes locally besom cats-effect extension
publish-local-cats:
	scala-cli --power publish local besom-cats --project-version {{publish-version}} --doc=false

# Publishes locally besom zio extension
publish-local-zio:
	scala-cli --power publish local besom-zio --project-version {{publish-version}} --doc=false

# Publishes locally all SDK modules: core, cats-effect extension, zio extension
publish-local-sdk: publish-local-core publish-local-cats publish-local-zio

# Publishes locally besom compiler plugin
publish-local-compiler-plugin:
	scala-cli --power publish local compiler-plugin --project-version {{publish-version}} --doc=false

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
clean-sdk: clean-core clean-cats clean-zio

# Cleans besom compiler plugin build
clean-compiler-plugin:
	scala-cli clean compiler-plugin

# Cleans the ./.out directory
clean-out:
	rm -rf ./.out

####################
# Language plugin
####################

# Builds .jar file with language plugin bootstrap library
build-bootstrap:
	mkdir -p {{language-plugin-output-dir}} && \
	scala-cli --power package language-plugin/bootstrap --assembly -o {{language-plugin-output-dir}}/bootstrap.jar -f

# Builds pulumi-language-scala binary
build-language-host:
	mkdir -p {{language-plugin-output-dir}} && \
	cd language-plugin/pulumi-language-scala && \
	go build -o {{language-plugin-output-dir}}/pulumi-language-scala

# Builds the entire scala language plugin
build-language-plugin: build-bootstrap build-language-host

# Runs the tests for the language plugin assuming it has already been built
run-language-plugin-tests:
	PULUMI_SCALA_PLUGIN_VERSION={{publish-version}} \
	PULUMI_SCALA_PLUGIN_LOCAL_PATH={{language-plugin-output-dir}} \
	scala-cli test language-plugin/tests/src

# Builds and tests the language plugin
test-language-plugin: build-language-plugin run-language-plugin-tests

# Installs the scala language plugin locally
install-language-plugin: build-language-plugin
	pulumi plugin rm language scala
	pulumi plugin install language scala {{publish-version}} --file {{language-plugin-output-dir}}


####################
# Codegen
####################

# Compiles codegen module
compile-codegen:
	scala-cli compile codegen

# Download the schema for a specific provider, e.g. `just get-schema kubernetes`
get-schema schema-name schema-version:
	#!/usr/bin/env sh
	pulumi plugin install resource {{schema-name}} {{schema-version}};
	schema_source={{ if schema-version == "" { schema-name } else { schema-name + "@" + schema-version } }}
	schema_dir="{{schemas-output-dir}}/{{schema-name}}/{{schema-version}}"
	mkdir -p $schema_dir
	pulumi package get-schema $schema_source > $schema_dir/schema.json

# Generate scala API code for the given provider, e.g. `just generate-provider-sdk kubernetes`
generate-provider-sdk schema-name schema-version:
	scala-cli run codegen -- {{schemas-output-dir}} {{codegen-output-dir}} {{schema-name}} {{schema-version}}

# Compiles the previously generated scala API code for the given provider, e.g. `just compile-provider-sdk kubernetes`
compile-provider-sdk schema-name:
	scala-cli compile {{codegen-output-dir}}/{{schema-name}}

# Compiles and publishes locally the previously generated scala API code for the given provider, e.g. `just publish-local-provider-sdk kubernetes`
publish-local-provider-sdk schema-name schema-version:
	scala-cli --power publish local {{codegen-output-dir}}/{{schema-name}}/{{schema-version}} --doc=false


####################
# Demo
####################

# Run the sample kubernetes Pulumi app that resides in ./experimental directory
liftoff: 
        cd experimental && \
        pulumi up --stack liftoff

# Reverts the deployment of experimental sample kubernetes Pulumi app from ./experimental directory
destroy-liftoff: 
        cd experimental && \
        pulumi destroy --stack liftoff -y

# Cleans the deployment of experimental sample kubernetes Pulumi app from ./experimental directory to the ground
clean-liftoff: destroy-liftoff
	cd experimental && \
	pulumi stack rm liftoff -y

# Cleans the deployment of ./experimental app completely, rebuilds core and kubernetes provider SDKs, deploys the app again
clean-slate-liftoff: clean-sdk clean-liftoff
	just generate-provider-sdk kubernetes 3.30.2 
	just publish-local-core
	just publish-local-provider-sdk kubernetes 3.30.2
	just liftoff

