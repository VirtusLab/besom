// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"strings"

	"github.com/virtuslab/besom/language-host/fsys"
)

type sbt struct{}

var _ scalaExecutorFactory = &sbt{}

func (s sbt) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	ok, err := s.isSbtProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./sbt", "sbt"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	return s.newSbtExecutor(cmd, opts.BootstrapLibJarPath)
}

func (sbt) isSbtProject(opts ScalaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "sbt") {
		return true, nil
	}
	sbtMarkers := []string{
		"project/build.properties",
		"build.sbt",
	}
	for _, p := range sbtMarkers {
		isSbt, err := fsys.FileExists(opts.WD, p)
		if err != nil {
			return false, err
		}
		if isSbt {
			return true, nil
		}
	}
	return false, nil
}

func (sbt) newSbtExecutor(cmd string, bootstrapLibJarPath string) (*ScalaExecutor, error) {
	pulginsSbtCommandParts := []string{
		// STDOUT needs to be clean of sbt output, because we expect a JSON with plugin results
		`; set outputStrategy := Some(StdoutOutput)`,
		`; set Compile / unmanagedJars += Attributed.blank(file("` + bootstrapLibJarPath + `"))`,
		`; runMain besom.bootstrap.PulumiPluginsDiscoverer`,
	}
	pulginsSbtCommand := strings.Join(pulginsSbtCommandParts, " ")

	return &ScalaExecutor{
		Cmd:        cmd,
		BuildArgs:  []string{"-batch", "compile"},
		RunArgs:    []string{"-batch", "run"},
		PluginArgs: []string{"-batch", "-error", pulginsSbtCommand},
	}, nil
}
