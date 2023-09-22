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
clean-all: clean-sdk clean-out clean-compiler-plugin clean-codegen

# Compiles everything
compile-all: compile-sdk compile-codegen build-language-plugin

####################
# Language SDK
####################

compile-pulumi-protobufs:
	scala-cli run ./scripts -M proto -- all

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
	scala-cli --power publish local core --project-version {{besom-version}}

# Publishes locally besom cats-effect extension
publish-local-cats: publish-local-core
	scala-cli --power publish local besom-cats --project-version {{besom-version}} {{scala-cli-main-options-cats}}

# Publishes locally besom zio extension
publish-local-zio: publish-local-core
	scala-cli --power publish local besom-zio --project-version {{besom-version}} {{scala-cli-main-options-zio}}

# Publishes locally all SDK modules: core, cats-effect extension, zio extension
publish-local-sdk: publish-local-core publish-local-cats publish-local-zio

# Publishes locally besom compiler plugin
publish-local-compiler-plugin:
	scala-cli --power publish local compiler-plugin --project-version {{besom-version}}

# Publishes core besom SDK
publish-maven-core:
	scala-cli publish core --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom cats-effect extension
publish-maven-cats:
	scala-cli publish besom-cats {{ scala-cli-main-options-cats }} --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom zio extension
publish-maven-zio:
	scala-cli publish besom-zio {{ scala-cli-main-options-zio }} --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom compiler plugin
publish-maven-compiler-plugin:
	scala-cli publish compiler-plugin --project-version {{besom-version}} {{publish-maven-auth-options}}

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

# Builds .jar file with language plugin bootstrap library
build-language-plugin-bootstrap:
	mkdir -p {{language-plugin-output-dir}} && \
	scala-cli --power package language-plugin/bootstrap --assembly -o {{language-plugin-output-dir}}/bootstrap.jar -f

# Builds pulumi-language-scala binary
build-language-host $GOOS="" $GOARCH="":
	mkdir -p {{language-plugin-output-dir}} && \
	cd language-plugin/pulumi-language-scala && \
	go build -o {{language-plugin-output-dir}}/pulumi-language-scala

# Builds the entire scala language plugin
build-language-plugin: build-language-plugin-bootstrap build-language-host

# Runs the tests for the language plugin assuming it has already been built
run-language-plugin-tests:
	PULUMI_SCALA_PLUGIN_VERSION={{besom-version}} \
	PULUMI_SCALA_PLUGIN_LOCAL_PATH={{language-plugin-output-dir}} \
	scala-cli test language-plugin/tests/src

# Builds and tests the language plugin
test-language-plugin: build-language-plugin run-language-plugin-tests

# Installs the scala language plugin locally
install-language-plugin:
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
package-language-plugin GOOS GOARCH:
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
	scala-cli run codegen -- {{schemas-output-dir}} {{codegen-output-dir}} {{schema-name}} {{schema-version}} {{besom-version}}

# Compiles the previously generated scala API code for the given provider, e.g. `just compile-provider-sdk kubernetes`
compile-provider-sdk schema-name:
	scala-cli compile {{codegen-output-dir}}/{{schema-name}}

# Compiles and publishes locally the previously generated scala API code for the given provider, e.g. `just publish-local-provider-sdk kubernetes`
publish-local-provider-sdk schema-name schema-version:
	scala-cli --power publish local {{codegen-output-dir}}/{{schema-name}}/{{schema-version}}

####################
# Integration testing
####################

# Runs integration tests for besom
test-compiler-plugin: publish-local-sdk publish-local-compiler-plugin
	scala-cli test integration-tests

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

