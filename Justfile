# Big idea behind using a Justfile is so that we can have modules like in sbt.

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