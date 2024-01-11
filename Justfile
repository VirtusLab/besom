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

# use bloop for publishing locally
bloop := "false"

# replace with a function when https://github.com/casey/just/pull/1069 is merged
scala-cli-coverage-options-core := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-core } else { "" }
scala-cli-test-options-core := scala-cli-coverage-options-core

scala-cli-coverage-options-cats := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-cats } else { "" }
scala-cli-test-options-cats := scala-cli-coverage-options-cats

scala-cli-coverage-options-zio := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-zio } else { "" }
scala-cli-test-options-zio := scala-cli-coverage-options-zio

publish-maven-auth-options := "--user env:OSSRH_USERNAME --password env:OSSRH_PASSWORD --gpg-key $PGP_KEY_ID --gpg-option --pinentry-mode --gpg-option loopback --gpg-option --passphrase --gpg-option $PGP_PASSWORD"

scala-cli-bloop-opts := "--jvm=17 --bloop-jvm=17 --bloop-java-opt=-XX:MaxHeapSize=32G --bloop-java-opt=-XX:+UseParallelGC --bloop-java-opt=-XX:-UseZGC"
scala-cli-direct-opts := "--server=false --javac-opt=-verbose --javac-opt=-J-XX:MaxHeapSize=32G --javac-opt=-J-XX:+UseParallelGC"
scala-cli-publish-local-opts := if bloop == "true" { scala-cli-bloop-opts } else { scala-cli-direct-opts }

# This list of available targets
default:
	@just --list

####################
# Aggregate tasks
####################

# Cleans everything
clean-all: clean-json clean-sdk clean-out clean-compiler-plugin clean-codegen clean-scripts clean-test-integration clean-test-templates clean-test-examples clean-test-markdown

# Compiles everything
compile-all: compile-json compile-sdk compile-codegen compile-compiler-plugin build-language-plugin compile-scripts

# Tests everything
test-all: test-json test-sdk test-codegen test-integration test-templates test-examples test-markdown

# Runs all necessary checks before committing
before-commit: compile-all test-all

####################
# Language SDK
####################

# Compiles the protobufs for the language SDK
compile-pulumi-protobufs:
	scala-cli run --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts/Proto.scala -- all

# Compiles core besom SDK
compile-core: publish-local-json
	scala-cli --power compile core --suppress-experimental-feature-warning

# Compiles besom cats-effect extension
compile-cats: publish-local-core
	scala-cli --power compile besom-cats --suppress-experimental-feature-warning

# Compiles besom zio extension
compile-zio: publish-local-core
	scala-cli --power compile besom-zio --suppress-experimental-feature-warning

# Compiles all SDK modules
compile-sdk: compile-core compile-cats compile-zio compile-compiler-plugin

# Compiles besom compiler plugin
compile-compiler-plugin:
	scala-cli --power compile compiler-plugin --suppress-experimental-feature-warning

# Runs tests for core besom SDK
test-core:
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-core}}; fi
	scala-cli --power test core {{ scala-cli-test-options-core }} --suppress-experimental-feature-warning

# Runs tests for besom cats-effect extension
test-cats: publish-local-core
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-cats}}; fi
	scala-cli --power test besom-cats {{ scala-cli-test-options-cats }} --suppress-experimental-feature-warning

# Runs tests for besom zio extension
test-zio: publish-local-core
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-zio}}; fi
	scala-cli --power test besom-zio {{ scala-cli-test-options-zio }} --suppress-experimental-feature-warning

# Runs all tests
test-sdk: compile-sdk test-core test-cats test-zio

# Publishes locally core besom SDK
publish-local-core: test-core
	scala-cli --power publish local core --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes locally besom cats-effect extension
publish-local-cats: publish-local-core
	scala-cli --power publish local besom-cats --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes locally besom zio extension
publish-local-zio: publish-local-core
	scala-cli --power publish local besom-zio --project-version {{besom-version}} --suppress-experimental-feature-warning

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
	scala-cli --power publish besom-cats --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom zio extension
publish-maven-zio:
	scala-cli --power publish besom-zio --project-version {{besom-version}} {{publish-maven-auth-options}}

# Publishes besom compiler plugin
publish-maven-compiler-plugin:
	scala-cli --power publish compiler-plugin --project-version {{besom-version}} {{publish-maven-auth-options}}

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

# Cleans codegen build
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
# Json
####################

# Compiles json module
compile-json:
    scala-cli --power compile besom-json --suppress-experimental-feature-warning

test-json:
    scala-cli --power test besom-json --suppress-experimental-feature-warning

clean-json:
    scala-cli --power clean besom-json

publish-local-json:
    scala-cli --power publish local besom-json --project-version {{besom-version}} --suppress-experimental-feature-warning

publish-maven-json:
    scala-cli --power publish besom-json --project-version {{besom-version}} {{publish-maven-auth-options}}

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
	scala-cli --power package language-plugin/bootstrap --suppress-experimental-feature-warning --assembly -o {{language-plugin-output-dir}}/bootstrap.jar -f

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
	pulumi --non-interactive --logtostderr plugin rm language scala -y
	pulumi --non-interactive --logtostderr plugin install language scala {{besom-version}} --file {{language-plugin-output-dir}}/local

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

# Runs tests for codegen
test-codegen:
	scala-cli --power test codegen --suppress-experimental-feature-warning

# Dowloads Pulumi Packages metadata for all providers in the registry
get-metadata-all:
	export GITHUB_TOKEN=$(gh auth token); \
	scala-cli run --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts/Packages.scala -- metadata-all

# Generates Scala SDKs for all providers in the registry
generate-provider-all:
	export GITHUB_TOKEN=$(gh auth token); \
	scala-cli run --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts/Packages.scala -- generate-all

# Publishes locally Scala SDKs for all providers in the registry
publish-local-provider-all:
    scala-cli run --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts/Packages.scala -- publish-local-all

# Download the schema for a specific provider, e.g. `just get-schema kubernetes 4.0.0`
get-schema schema-name schema-version:
	#!/usr/bin/env sh
	pulumi --non-interactive --logtostderr plugin install resource {{schema-name}} {{schema-version}};
	schema_source={{ if schema-version == "" { schema-name } else { schema-name + "@" + schema-version } }}
	schema_dir="{{schemas-output-dir}}/{{schema-name}}/{{schema-version}}"
	mkdir -p $schema_dir
	pulumi --non-interactive --logtostderr package get-schema $schema_source > $schema_dir/schema.json

# Publishes locally besom codegen
publish-local-codegen: test-codegen
	scala-cli --power publish local codegen --project-version {{besom-version}} --suppress-experimental-feature-warning

# Generate scala API code for the given provider, e.g. `just generate-provider-sdk kubernetes 4.0.0`
generate-provider-sdk schema-name schema-version:
	scala-cli --power run codegen --suppress-experimental-feature-warning -- named {{schema-name}} {{schema-version}}

# Compiles the previously generated scala API code for the given provider, e.g. `just compile-provider-sdk kubernetes 4.0.0`
compile-provider-sdk schema-name schema-version:
	scala-cli --power compile {{scala-cli-publish-local-opts}} {{codegen-output-dir}}/{{schema-name}}/{{schema-version}} --suppress-experimental-feature-warning --interactive=false

# Compiles and publishes locally the previously generated scala API code for the given provider, e.g. `just publish-local-provider-sdk kubernetes 4.0.0`
publish-local-provider-sdk schema-name schema-version:
	scala-cli --power publish local --sources=false --doc=false {{scala-cli-publish-local-opts}} {{codegen-output-dir}}/{{schema-name}}/{{schema-version}} --suppress-experimental-feature-warning

# Compiles and publishes the previously generated scala API code for the given provider, e.g. `just publish-maven-provider-sdk kubernetes 4.0.0`
publish-maven-provider-sdk schema-name schema-version:
	scala-cli --power publish {{codegen-output-dir}}/{{schema-name}}/{{schema-version}} {{publish-maven-auth-options}}

####################
# Integration testing
####################

# Runs all integration tests
test-integration: test-integration-core test-integration-compiler-plugin test-integration-codegen test-integration-language-plugin
	scala-cli --power test integration-tests

test-integration-ci: publish-local-codegen publish-local-sdk install-language-plugin publish-local-compiler-plugin
	scala-cli --power test integration-tests

# Cleans after integration tests
clean-test-integration: clean-test-integration-codegen
	scala-cli --power clean integration-tests

# Runs integration tests for core
test-integration-core: publish-local-codegen publish-local-sdk install-language-plugin publish-local-compiler-plugin
	scala-cli --power test integration-tests --test-only 'besom.integration.core*'

# Runs integration tests for compiler plugin
test-integration-compiler-plugin: publish-local-codegen publish-local-core install-language-plugin publish-local-compiler-plugin
	scala-cli --power test integration-tests --test-only 'besom.integration.compilerplugin*'

# Runs integration tests for language plugin
test-integration-language-plugin: publish-local-codegen publish-local-core install-language-plugin publish-local-compiler-plugin
	export GITHUB_TOKEN=$(gh auth token); \
	scala-cli --power test integration-tests --test-only 'besom.integration.languageplugin*'

# Runs integration tests for codegen
test-integration-codegen: publish-local-codegen
	export GITHUB_TOKEN=$(gh auth token); \
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
	pulumi --non-interactive --logtostderr --color=never --emoji=false new -y --force --dir target/test/{{template-name}} -n templates-test-{{template-name}} --stack templates-test-{{template-name}} ../../../templates/{{template-name}}/
	scala-cli compile target/test/{{template-name}}
	@echo "----------------------------------------"

# Cleans after a template test
clean-test-template template-name:
	@echo "Cleaning template test for {{template-name}}"
	scala-cli clean target/test/{{template-name}} || echo "Could not clean"
	pulumi --non-interactive --logtostderr --color=never --emoji=false stack rm --cwd target/test/{{template-name}} -y || echo "No stack to remove"
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
# Scripts
####################

# Compiles scripts module
compile-scripts: publish-local-codegen
	scala-cli --power compile scripts --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning

# Clean scripts module
clean-scripts:
    scala-cli --power clean scripts

bump-version new-version:
    scala-cli run scripts/Version.scala -- bump {{new-version}}

####################
# Troubleshooting
####################

# Cleans everything, including the local ivy, git untracked files, and kills all java processes
power-wash: clean-all
	rm -rf ~/.ivy2/local/org.virtuslab/
	git clean -i -d -x
	killall -9 java

####################
# Demo
####################

# Run the sample kubernetes Pulumi app that resides in ./experimental directory
liftoff:
	#!/usr/bin/env sh
	export PULUMI_CONFIG_PASSPHRASE=""
	cd experimental
	pulumi --non-interactive --logtostderr up --stack liftoff -y

# Reverts the deployment of experimental sample kubernetes Pulumi app from ./experimental directory
destroy-liftoff:
	#!/usr/bin/env sh
	cd experimental
	if (pulumi --non-interactive --logtostderr stack ls | grep liftoff > /dev/null); then
		export PULUMI_CONFIG_PASSPHRASE=""
		pulumi  --non-interactive --logtostderr destroy --stack liftoff -y
	fi

# Cleans the deployment of experimental sample kubernetes Pulumi app from ./experimental directory to the ground
clean-liftoff: destroy-liftoff
	#!/usr/bin/env sh
	cd experimental
	if (pulumi --non-interactive --logtostderr stack ls | grep liftoff > /dev/null); then
		pulumi --non-interactive --logtostderr stack rm liftoff -y
	fi
	export PULUMI_CONFIG_PASSPHRASE=""
	pulumi --non-interactive --logtostderr stack init liftoff

# Cleans the deployment of ./experimental app completely, rebuilds core and kubernetes provider SDKs, deploys the app again
clean-slate-liftoff: clean-sdk
	#!/usr/bin/env sh
	just generate-provider-sdk kubernetes 4.2.0 
	just publish-local-core
	just publish-local-compiler-plugin
	just publish-local-provider-sdk kubernetes 4.2.0
	just clean-liftoff
	just liftoff

####################
# IDE
####################

# Runs 'scala-cli setup-ide' for all modules
setup-intellij:
	for file in `ls */project.scala | cut -f1 -d'/'`; do scala-cli setup-ide $file --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning; done
	for file in `ls */*/project.scala | cut -f1,2 -d'/'`; do scala-cli setup-ide $file --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning; done
