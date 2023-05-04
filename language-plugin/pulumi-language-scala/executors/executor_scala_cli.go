// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
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
	return s.newScalaCliExecutor(cmd, opts.BootstrapLibJarPath)
}

func (scalacli) newScalaCliExecutor(cmd string, bootstrapLibJarPath string) (*ScalaExecutor, error) {
	return &ScalaExecutor{
		Cmd:        cmd,
		BuildArgs:  []string{"compile", "."},
		RunArgs:    []string{"run", "."},
		PluginArgs: []string{"run", ".", "--jar", bootstrapLibJarPath, "--main-class", "besom.bootstrap.PulumiPluginsDiscoverer"},
	}, nil
}
