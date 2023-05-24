# Big idea behind using a Justfile is so that we can have modules like in sbt.

publish-version := "0.0.1-SNAPSHOT"
language-plugin-output-dir := justfile_directory() + "/.out/language-plugin"
codegen-output-dir := justfile_directory() + "/.out/codegen"
schemas-output-dir := justfile_directory() + "/.out/schemas"

# This list of available targets
default:
    @just --list


####################
# Language SDK
####################

# Compiles core besom SDK
compile-core:
	scala-cli compile core

# Compiles besom cats-effect extension
compile-cats:
	scala-cli compile besom-cats

# Compiles besom zio extension
compile-zio:
	scala-cli compile besom-zio

# Compiles all SDK modules
compile-sdk: compile-core compile-cats compile-zio

# Runs tests for core besom SDK
test-core:
	scala-cli test core

# Runs tests for besom cats-effect extension
test-cats:
	scala-cli test besom-cats

# Runs tests for besom zio extension
test-zio:
  scala-cli test besom-zio

# Runs all tests
test-sdk: test-core test-cats test-zio

# Publishes locally core besom SDK
publish-local-core:
  scala-cli publish local core --version {{publish-version}} --doc=false


####################
# Language plugin
####################

# Builds .jar file with language plugin bootstrap library
build-bootstrap:
	mkdir -p {{language-plugin-output-dir}} && \
	scala-cli package language-plugin/bootstrap --assembly -o {{language-plugin-output-dir}}/bootstrap.jar -f

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
get-schema schema-name schema-version='':
	#!/usr/bin/env sh
	mkdir -p {{schemas-output-dir}}
	pulumi plugin install resource {{schema-name}} {{schema-version}};
	schema_source={{ if schema-version == "" { schema-name } else { schema-name + "@" + schema-version } }}
	pulumi package get-schema $schema_source > {{schemas-output-dir}}/{{schema-name}}.json

# Generate scala API code for the given provider, e.g. `just generate-provider-sdk kubernetes`
generate-provider-sdk schema-name schema-version='':
	just get-schema {{schema-name}} {{schema-version}}
	rm -rf {{codegen-output-dir}}/{{schema-name}}
	scala-cli run codegen -- {{schemas-output-dir}} {{codegen-output-dir}} {{schema-name}}

# Compiles the previously generated scala API code for the given provider, e.g. `just compile-provider-sdk kubernetes`
compile-provider-sdk schema-name:
	scala-cli compile {{codegen-output-dir}}/{{schema-name}}

# Compiles and publishes locally the previously generated scala API code for the given provider, e.g. `just publish-local-provider-sdk kubernetes`
publish-local-provider-sdk schema-name:
	scala-cli publish local {{codegen-output-dir}}/{{schema-name}} --doc=false


####################
# Demo
####################

liftoff: publish-local-core
        cd experimental && \
        pulumi up --stack liftoff
