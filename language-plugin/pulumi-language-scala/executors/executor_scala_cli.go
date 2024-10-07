// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"os"

	"github.com/virtuslab/besom/language-host/fsys"
)

type scalacli struct{}

var _ scalaExecutorFactory = &scalacli{}

func (s scalacli) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./scala-cli", "scala-cli"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	pluginDiscovererOutputPath := PluginDiscovererOutputFilePath(opts.WD)
	return s.newScalaCliExecutor(cmd, opts.BootstrapLibJarPath, pluginDiscovererOutputPath)
}

func (scalacli) newScalaCliExecutor(cmd string, bootstrapLibJarPath string, pluginDiscovererOutputPath string) (*ScalaExecutor, error) {
	scalaCliOpts := os.Getenv("BESOM_LANGHOST_SCALA_CLI_OPTS")
	return &ScalaExecutor{
		Name:        "scala-cli",
		Cmd:         cmd,
		BuildArgs:   []string{"compile", ".", scalaCliOpts},
		RunArgs:     []string{"run", ".", scalaCliOpts},
		PluginArgs:  []string{"run", ".", scalaCliOpts, "--jar", bootstrapLibJarPath, "--main-class", "besom.bootstrap.PulumiPluginsDiscoverer", "--", "--output-file", pluginDiscovererOutputPath},
		VersionArgs: []string{"version", "--cli", "--offline"},
	}, nil
}
