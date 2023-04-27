# Big idea behind using a Justfile is so that we can have modules like in sbt.

publish-version := "0.0.1-SNAPSHOT"
language-plugin-output-dir := justfile_directory() + "/.out/language-plugin"

# This list of available targets
default:
    @just --list

# Compiles core besom SDK
compile-core:
	scala-cli compile core

# Compiles besom cats-effect extension
compile-cats:
	scala-cli compile besom-cats

# Compiles besom zio extension
compile-zio:
	scala-cli compile besom-zio

# Compiles all modules
compile: compile-core compile-cats compile-zio

# Publishes locally core besom SDK
publish-local-core:
  scala-cli publish local core --version {{publish-version}}

# Builds .jar file with language plugin bootstrap library
build-bootstrap:
	mkdir -p {{language-plugin-output-dir}} && \
	scala-cli package language-plugin/bootstrap --assembly -o {{language-plugin-output-dir}}/bootstrap.jar -f

# Builds pulumi-language-scala binary
build-language-host:
	mkdir -p {{language-plugin-output-dir}} && \
	cd language-plugin/pulumi-language-scala && \
	go build -o {{language-plugin-output-dir}}/pulumi-language-scala

# Installs locally scala language plugin
install-language-plugin: build-bootstrap build-language-host
	pulumi plugin rm language scala
	pulumi plugin install language scala {{publish-version}} --file {{language-plugin-output-dir}}

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
test: test-core test-cats test-zio
