# Big idea behind using a Justfile is so that we can have modules like in sbt.

publish-version := "0.0.1-SNAPSHOT"
language-plugin-output-dir := justfile_directory() + "/.out/language-plugin"

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
  scala-cli publish local core --version {{publish-version}}


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

# TEMP

liftoff: publish-local-core
        cd experimental && \
        pulumi up --stack liftoff
