// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"fmt"

	"github.com/virtuslab/besom/language-host/fsys"
)

// Abstracts interactions with a Scala project, ability to build, run
// Scala code, and detect plugin dependencies.
type ScalaExecutor struct {
	// Path to the command to run.
	Cmd string

	// Optional dir to run the command in.
	Dir string

	// Command to run the Scala code - the main entrypoint.
	RunArgs []string

	// Command args to resolve dependencies and build; this will
	// be called after `pulumi new` on Gradle templates. Optional.
	BuildArgs []string

	// Command to autodetect and print Pulumi plugins depended on
	// by the Scala program.
	PluginArgs []string
}

// Information available to pick an executor.
type ScalaExecutorOptions struct {
	// Current working directory. Abstract to enable testing.
	WD fsys.ParentFS

	// The absolute path to the bootstrap library jar
	// which should be shipped together with the main language host binary
	BootstrapLibJarPath string

	// The value of `runtime.options.binary` setting from
	// `Pulumi.yaml`. Optional.
	Binary string

	// The value of `runtime.options.use-executor` setting from
	// `Pulumi.yaml`. Optional.
	UseExecutor string
}

type scalaExecutorFactory interface {
	// Tries configuring an executor from the given options. May
	// return nil if options are not-applicable.
	NewScalaExecutor(ScalaExecutorOptions) (*ScalaExecutor, error)
}

func NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	e, err := combineScalaExecutorFactories(
		&jarexec{},
		&sbt{},
		&scalacli{},
	).NewScalaExecutor(opts)
	if err != nil {
		return nil, err
	}
	if e == nil {
		return nil, fmt.Errorf("failed to configure executor, tried: scala-cli, sbt, jar")
	}
	return e, nil
}

type combinedScalaExecutorFactory []scalaExecutorFactory

func (c combinedScalaExecutorFactory) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	for _, v := range c {
		e, err := v.NewScalaExecutor(opts)
		if err != nil {
			return nil, err
		}
		if e != nil {
			return e, nil
		}
	}
	return nil, nil
}

func combineScalaExecutorFactories(variations ...scalaExecutorFactory) scalaExecutorFactory {
	return combinedScalaExecutorFactory(variations)
}
