# Big idea behind using a Justfile is so that we can have modules like in sbt.

besom-version := `cat version.txt`
besom-cfg-version := `cat besom-cfg/version.txt`
is-snapshot := if "{{besom-version}}" =~ '.*-SNAPSHOT' { "true" } else { "false" }
no-bloop-ci := if env_var_or_default('CI', "") == "true" { "--server=false" } else { "" }

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

scala-cli-coverage-options-cats := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-cats } else { "" }
scala-cli-test-options-cats := scala-cli-coverage-options-cats

scala-cli-coverage-options-zio := if coverage == "true" { "-O -coverage-out:" + coverage-output-dir-zio } else { "" }
scala-cli-test-options-zio := scala-cli-coverage-options-zio

publish-maven-auth-options := "--user env:OSSRH_USERNAME --password env:OSSRH_PASSWORD --gpg-key $PGP_KEY_ID --gpg-option --pinentry-mode --gpg-option loopback --gpg-option --passphrase --gpg-option $PGP_PASSWORD"
ci-opts := if env_var_or_default('CI', "") == "true" { "--repository=sonatype:snapshots" } else { "" }

# This list of available targets
default:
	@just --list

####################
# Aggregate tasks
####################

# Cleans everything
clean-all: clean-json clean-sdk clean-auto clean-out clean-compiler-plugin clean-codegen clean-scripts clean-test-integration clean-cfg clean-test-templates clean-test-examples clean-test-markdown

# Compiles everything
compile-all: compile-json compile-sdk compile-auto compile-codegen compile-scripts compile-compiler-plugin build-language-plugin

# Tests everything
test-all: test-json test-sdk test-auto test-codegen test-scripts test-integration test-cfg test-templates test-examples test-markdown

# Publishes everything locally
publish-local-all: publish-local-json publish-local-sdk publish-local-auto publish-local-codegen publish-local-cfg publish-local-scripts install-language-plugin

# Publishes everything to Maven
publish-maven-all: publish-maven-json publish-maven-sdk publish-maven-auto publish-maven-codegen publish-maven-scripts

# Runs all necessary checks before committing
before-commit: compile-all test-all

####################
# Language SDK
####################

# Compiles core besom SDK
compile-core: publish-local-json
	scala-cli --power compile {{no-bloop-ci}} core --suppress-experimental-feature-warning

# Compiles besom cats-effect extension
compile-cats: publish-local-core
	scala-cli --power compile {{no-bloop-ci}} besom-cats --suppress-experimental-feature-warning

# Compiles besom zio extension
compile-zio: publish-local-core
	scala-cli --power compile {{no-bloop-ci}} besom-zio --suppress-experimental-feature-warning

# Compiles all SDK modules
compile-sdk: compile-core compile-cats compile-zio compile-compiler-plugin

# Compiles besom compiler plugin
compile-compiler-plugin:
	scala-cli --power compile {{no-bloop-ci}} compiler-plugin --suppress-experimental-feature-warning

# Runs tests for core besom SDK
test-core: compile-core
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-core}}; fi
	scala-cli --power test {{no-bloop-ci}} core {{ scala-cli-test-options-core }} --suppress-experimental-feature-warning

# Runs tests for besom cats-effect extension
test-cats: publish-local-core
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-cats}}; fi
	scala-cli --power test {{no-bloop-ci}} besom-cats {{ scala-cli-test-options-cats }} --suppress-experimental-feature-warning

# Runs tests for besom zio extension
test-zio: publish-local-core
	@if [ {{ coverage }} = "true" ]; then mkdir -p {{coverage-output-dir-zio}}; fi
	scala-cli --power test {{no-bloop-ci}} besom-zio {{ scala-cli-test-options-zio }} --suppress-experimental-feature-warning

# Runs all tests
test-sdk: test-core test-cats test-zio

# Publishes locally all SDK modules
publish-local-sdk: publish-local-core publish-local-cats publish-local-zio publish-local-compiler-plugin

# Publishes to maven all SDK modules
publish-maven-sdk: publish-maven-core publish-maven-cats publish-maven-zio publish-maven-compiler-plugin

# Publishes locally core besom SDK
publish-local-core: publish-local-json
	scala-cli --power publish local {{no-bloop-ci}} core --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes locally besom cats-effect extension
publish-local-cats: publish-local-core
	scala-cli --power publish local {{no-bloop-ci}} besom-cats --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes locally besom zio extension
publish-local-zio: publish-local-core
	scala-cli --power publish local {{no-bloop-ci}} besom-zio --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes locally besom compiler plugin
publish-local-compiler-plugin:
	scala-cli --power publish local {{no-bloop-ci}} compiler-plugin --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes core besom SDK to Maven
publish-maven-core:
	scala-cli --power publish {{no-bloop-ci}} core --project-version {{besom-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

# Publishes besom cats-effect extension to Maven
publish-maven-cats:
	scala-cli --power publish {{no-bloop-ci}} besom-cats --project-version {{besom-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

# Publishes besom zio extension to Maven
publish-maven-zio:
	scala-cli --power publish {{no-bloop-ci}} besom-zio --project-version {{besom-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

# Publishes besom compiler plugin to Maven
publish-maven-compiler-plugin:
	scala-cli --power publish {{no-bloop-ci}} compiler-plugin --project-version {{besom-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

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
	scala-cli --power compile {{no-bloop-ci}} besom-json --suppress-experimental-feature-warning

# Runs tests for json module
test-json:
	scala-cli --power test {{no-bloop-ci}} besom-json --suppress-experimental-feature-warning

# Cleans json module
clean-json:
	scala-cli --power clean besom-json

# Publishes locally json module
publish-local-json:
	scala-cli --power publish local {{no-bloop-ci}} besom-json --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes json module to Maven
publish-maven-json:
	scala-cli --power publish {{no-bloop-ci}} besom-json --project-version {{besom-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning


####################
# Auto
####################

# Compiles auto module
compile-auto: publish-local-core
    scala-cli --power compile auto --suppress-experimental-feature-warning

# Runs tests for auto module
test-auto: compile-auto
    scala-cli --power test auto --suppress-experimental-feature-warning

# Cleans auto module
clean-auto:
    scala-cli --power clean auto

# Publishes locally auto module
publish-local-auto: test-auto
    scala-cli --power publish local auto --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes auto module
publish-maven-auto: test-auto
    scala-cli --power publish auto --project-version {{besom-version}} {{publish-maven-auth-options}}


####################
# Language plugin
####################

# Run go mod tidy for language plugin
tidy-language-plugin:
    cd language-plugin/pulumi-language-scala && \
    go mod tidy

# Packages .jar file with language plugin bootstrap library
package-language-plugin-bootstrap:
	mkdir -p {{language-plugin-output-dir}} && \
	scala-cli --power package {{no-bloop-ci}}  language-plugin/bootstrap --suppress-experimental-feature-warning --assembly -o {{language-plugin-output-dir}}/bootstrap.jar -f

# Builds pulumi-language-scala binary
build-language-plugin $GOOS="" $GOARCH="":
	mkdir -p {{language-plugin-output-dir}} && \
	cd language-plugin/pulumi-language-scala && \
	go build -o {{language-plugin-output-dir}}/pulumi-language-scala \
	  -ldflags "-X github.com/pulumi/pulumi/sdk/v3/go/common/version.Version={{besom-version}}"

# Installs the scala language plugin locally
install-language-plugin: build-language-plugin
	#!/usr/bin/env sh
	just package-language-plugin-bootstrap
	just build-language-plugin
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
	just build-language-plugin $GOOS $GOARCH
	cp {{language-plugin-output-dir}}/bootstrap.jar $output_dir/
	cp {{language-plugin-output-dir}}/pulumi-language-scala $output_dir/
	cd $output_dir
	tar czvf pulumi-language-scala-v{{besom-version}}-{{GOOS}}-{{GOARCH}}.tar.gz *

# Package the Besom scala language plugin for all supported architectures
package-language-plugins-all: package-language-plugin-bootstrap
	just package-language-plugin darwin arm64
	just package-language-plugin darwin amd64
	just package-language-plugin linux arm64
	just package-language-plugin linux amd64
	just package-language-plugin windows arm64
	just package-language-plugin windows amd64

# Publishes the scala language plugin to GitHub Packages
publish-language-plugin $GOOS $GOARCH:
	#!/usr/bin/env sh
	subdir={{ "dist/" + GOOS + "-" + GOARCH }}
	output_dir={{language-plugin-output-dir}}/$subdir
	gh release upload v{{besom-version}} $output_dir/pulumi-language-scala-v{{besom-version}}-{{GOOS}}-{{GOARCH}}.tar.gz --clobber

# Publishes the scala language plugin to GitHub Packages for all supported architectures
publish-language-plugins-all: package-language-plugins-all
	just publish-language-plugin darwin arm64
	just publish-language-plugin darwin amd64
	just publish-language-plugin linux arm64
	just publish-language-plugin linux amd64
	just publish-language-plugin windows arm64
	just publish-language-plugin windows amd64

####################
# Besom CFG
####################

# Compiles besom-cfg lib module
compile-cfg-lib: publish-local-json publish-local-core
	scala-cli --power compile besom-cfg/lib --suppress-experimental-feature-warning

# Compiles besom-cfg k8s module
compile-cfg-k8s: publish-local-cfg-lib
	just cli packages local kubernetes:4.10.0
	scala-cli --power compile besom-cfg/k8s --suppress-experimental-feature-warning

# Compiles all besom-cfg modules
compile-cfg: compile-cfg-lib compile-cfg-k8s

# Publishes locally besom-cfg lib module
publish-local-cfg-lib:
	scala-cli --power publish local besom-cfg/lib --project-version {{besom-cfg-version}} --suppress-experimental-feature-warning

# Publishes locally besom-cfg k8s module
publish-local-cfg-k8s:
	scala-cli --power publish local besom-cfg/k8s --project-version {{besom-cfg-version}} --suppress-experimental-feature-warning

# Publishes besom-cfg lib module to Maven
publish-maven-cfg-lib:
	scala-cli --power publish besom-cfg/lib --project-version {{besom-cfg-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

# Publishes besom-cfg k8s module to Maven
publish-maven-cfg-k8s:
	scala-cli --power publish besom-cfg/k8s --project-version {{besom-cfg-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

# Tests besom-cfg lib module
test-cfg-lib: compile-cfg-lib
	scala-cli --power test besom-cfg/lib --suppress-experimental-feature-warning

# Tests besom-cfg k8s module
test-cfg-k8s: publish-local-cfg-lib compile-cfg-k8s
	scala-cli --power test besom-cfg/k8s --suppress-experimental-feature-warning

# Runs all tests of besom-cfg
test-cfg: test-cfg-lib test-cfg-k8s

# Cleans besom-cfg-lib build
clean-cfg-lib:
	scala-cli clean besom-cfg/lib

# Cleans besom-cfg-k8s build
clean-cfg-k8s:
	scala-cli clean besom-cfg/k8s

# Cleans all besom-cfg builds
clean-cfg: clean-cfg-lib clean-cfg-k8s

####################
# Codegen
####################

# Compiles Besom codegen module
compile-codegen:
	scala-cli --power compile {{no-bloop-ci}} codegen --suppress-experimental-feature-warning

# Runs tests for Besom codegen
test-codegen:
	scala-cli --power test {{no-bloop-ci}} codegen --suppress-experimental-feature-warning

# Cleans codegen build
clean-codegen:
	scala-cli clean codegen

# Publishes locally Besom codegen
publish-local-codegen: test-codegen
	scala-cli --power publish local {{no-bloop-ci}} codegen --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes Besom codegen
publish-maven-codegen: test-codegen
	scala-cli --power publish {{no-bloop-ci}} codegen --project-version {{besom-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

####################
# Integration testing
####################

# Runs all integration tests
test-integration: test-integration-core test-integration-compiler-plugin test-integration-codegen test-integration-language-plugin
	scala-cli --power test {{no-bloop-ci}} integration-tests

# Cleans after integration tests
clean-test-integration: clean-test-integration-codegen
	scala-cli --power clean integration-tests

# Runs integration tests for core
test-integration-core: publish-local-codegen publish-local-sdk install-language-plugin publish-local-compiler-plugin
	scala-cli --power test {{no-bloop-ci}} integration-tests --test-only 'besom.integration.core*'

# Runs integration tests for compiler plugin
test-integration-compiler-plugin: publish-local-codegen publish-local-core install-language-plugin publish-local-compiler-plugin
	scala-cli --power test {{no-bloop-ci}} integration-tests --test-only 'besom.integration.compilerplugin*'

# Runs integration tests for language plugin
test-integration-language-plugin: publish-local-codegen publish-local-core install-language-plugin publish-local-compiler-plugin
	export GITHUB_TOKEN=$(gh auth token); \
	scala-cli --power test {{no-bloop-ci}} integration-tests --test-only 'besom.integration.languageplugin*'

# Runs fast integration tests for codegen
test-integration-codegen: publish-local-core publish-local-codegen
	export GITHUB_TOKEN=$(gh auth token); \
	scala-cli --power test {{no-bloop-ci}} integration-tests --test-only 'besom.integration.codegen*'

# Runs fast&slow integration tests for codegen
test-integration-codegen-slow: publish-local-core publish-local-codegen
	export GITHUB_TOKEN=$(gh auth token); \
	scala-cli --power test {{no-bloop-ci}} integration-tests --test-only 'besom.integration.codegen*' -- --include-categories=Slow

# Cleans after the codegen integration tests
clean-test-integration-codegen:
	rm -rf integration-tests/resources/testdata/*/codegen

# Copies test schemas from pulumi repo to the testdata directory
copy-test-schemas:
	scala-cli run {{no-bloop-ci}} --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts -- schemas all

####################
# Templates and examples
####################

# Runs a template test
test-template template-name:
	@echo "----------------------------------------"
	@echo "Testing template {{template-name}}"
	pulumi --non-interactive --logtostderr --color=never --emoji=false new -y --force --generate-only --dir target/test/{{template-name}} -n templates-test-{{template-name}} --stack templates-test-{{template-name}} ../../../templates/{{template-name}}/
	scala-cli compile {{no-bloop-ci}} target/test/{{template-name}} {{ci-opts}} --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning

# Cleans after a template test
clean-test-template template-name:
	@echo "----------------------------------------"
	@echo "Cleaning template test for {{template-name}}"
	scala-cli clean target/test/{{template-name}} || echo "Could not clean"
	pulumi --non-interactive --logtostderr --color=never --emoji=false stack rm --cwd target/test/{{template-name}} -y || echo "No stack to remove"
	rm -rf ./target/test/{{template-name}} || echo "No directory to remove"
	rm -rf $HOME/.pulumi/stacks/templates-test-{{template-name}} || echo "No directory to remove"

# Runs all template tests
test-templates:
	for file in `ls -d templates/*/ | cut -f2 -d'/'`; do just test-template $file || exit 1; done

# Cleans after template tests
clean-test-templates:
	for file in `ls -d templates/*/ | cut -f2 -d'/'`; do just clean-test-template $file; done

# Runs an example test
test-example example-name:
	@echo "----------------------------------------"
	@echo "Testing example {{example-name}}"
	scala-cli compile {{no-bloop-ci}} examples/{{example-name}} {{ci-opts}} --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning

# Cleans after an example test
clean-test-example example-name:
	@echo "----------------------------------------"
	@echo "Cleaning example test for {{example-name}}"
	scala-cli clean examples/{{example-name}}

# Runs all template tests
test-examples:
	for file in `ls -d examples/*/ | cut -f2 -d'/'`; do just test-example $file || exit 1; done

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

# Compiles Besom scripts module
compile-scripts: publish-local-codegen
	scala-cli --power compile {{no-bloop-ci}} scripts --suppress-experimental-feature-warning

# Clean Besom scripts module
clean-scripts:
	scala-cli --power clean scripts

# Runs tests for Besom scripts
test-scripts:
	scala-cli --power test {{no-bloop-ci}} scripts --suppress-experimental-feature-warning

# Publishes locally Besom scripts module
publish-local-scripts: test-scripts
	scala-cli --power publish local {{no-bloop-ci}} scripts --project-version {{besom-version}} --suppress-experimental-feature-warning

# Publishes Besom scripts module
publish-maven-scripts: test-scripts
	scala-cli --power publish {{no-bloop-ci}} scripts --project-version {{besom-version}} {{publish-maven-auth-options}} --suppress-experimental-feature-warning

# Use Besom scripts directly
cli *ARGS:
	scala-cli run {{no-bloop-ci}} scripts {{ci-opts}} --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning scripts -- {{ARGS}}

# Create or Update GitHub release
upsert-gh-release:
	#!/usr/bin/env sh
	if {{is-snapshot}}; then
		echo "Not a snapshot version, refusing to delete a release"
	else
		gh release delete v{{besom-version}} --yes || echo "Nothing to delete"
	fi
	echo Creating release v{{besom-version}}
	gh release create v{{besom-version}} --title v{{besom-version}} --notes "" --prerelease --draft

####################
# Troubleshooting
####################

# Cleans everything, including the local ivy, git untracked files, and kills all java processes
power-wash: clean-all
	rm -rf ~/.ivy2/local/org.virtuslab/
	rm -rf ~/.m2/repository/org/virtuslab/
	rm -rf ~/.cache/coursier/v1/https/repo1.maven.org/maven2/org/virtuslab/ # Linux
	rm -rf ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/virtuslab/ # Mac
	git clean -i -d -x -e ".idea"
	killall -9 java

####################
# Demo
####################

# Run the sample kubernetes Pulumi app that resides in ./experimental directory
liftoff:
	#!/usr/bin/env sh
	export PULUMI_SKIP_UPDATE_CHECK=true
	export PULUMI_CONFIG_PASSPHRASE=""
	cd experimental
	pulumi --non-interactive --logtostderr up --stack liftoff -y

# Reverts the deployment of experimental sample kubernetes Pulumi app from ./experimental directory
destroy-liftoff:
	#!/usr/bin/env sh
	export PULUMI_SKIP_UPDATE_CHECK=true
	cd experimental
	if (pulumi --non-interactive --logtostderr stack ls | grep liftoff > /dev/null); then
		export PULUMI_CONFIG_PASSPHRASE=""
		pulumi  --non-interactive --logtostderr destroy --stack liftoff -y
	fi

# Cleans the deployment of experimental sample kubernetes Pulumi app from ./experimental directory to the ground
clean-liftoff: destroy-liftoff
	#!/usr/bin/env sh
	export PULUMI_SKIP_UPDATE_CHECK=true
	cd experimental
	if (pulumi --non-interactive --logtostderr stack ls | grep liftoff > /dev/null); then
		pulumi --non-interactive --logtostderr stack rm liftoff -y
	fi
	export PULUMI_CONFIG_PASSPHRASE=""
	pulumi --non-interactive --logtostderr stack init liftoff

# Cleans the deployment of ./experimental app completely, rebuilds core and kubernetes provider SDKs, deploys the app again
clean-slate-liftoff: clean-sdk
	#!/usr/bin/env sh
	just publish-local-core
	just publish-local-compiler-plugin
	scala-cli run {{no-bloop-ci}} codegen -- named kubernetes 4.2.0
	scala-cli --power publish local {{no-bloop-ci}} .out/codegen/kubernetes/4.2.0/ --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning
	just clean-liftoff
	just liftoff

####################
# IDE
####################

# Runs 'scala-cli setup-ide' for all modules
setup-intellij:
	for file in `ls */project.scala | cut -f1 -d'/'`; do scala-cli setup-ide $file --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning; done
	for file in `ls */*/project.scala | cut -f1,2 -d'/'`; do scala-cli setup-ide $file --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning; done
	for file in `ls */*/*/project.scala | cut -f1,2,3 -d'/'`; do scala-cli setup-ide $file --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning; done
