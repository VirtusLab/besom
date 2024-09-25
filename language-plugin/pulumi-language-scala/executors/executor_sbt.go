// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"fmt"
	"os"
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
	pluginDiscovererOutputPath := PluginDiscovererOutputFilePath(opts.WD)
	return s.newSbtExecutor(cmd, opts.BootstrapLibJarPath, pluginDiscovererOutputPath)
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

func (sbt) newSbtExecutor(cmd string, bootstrapLibJarPath string, pluginDiscovererOutputPath string) (*ScalaExecutor, error) {
	sbtModule := os.Getenv("BESOM_SBT_MODULE")

	se := &ScalaExecutor{
		Name:        "sbt",
		Cmd:         cmd,
		RunArgs:     makeArgs(sbtModule, "run"),
		BuildArgs:   makeArgs(sbtModule, "compile"),
		PluginArgs:  []string{"-batch", makePluginsSbtCommandParts(sbtModule, bootstrapLibJarPath, pluginDiscovererOutputPath)},
		VersionArgs: []string{"--numeric-version"},
	}

	return se, nil
}

func makePluginsSbtCommandParts(sbtModule string, bootstrapLibJarPath string, pluginDiscovererOutputPath string) string {
	if sbtModule != "" {
		sbtModule = sbtModule + " / "
	}

	pluginsSbtCommandParts := []string{
		fmt.Sprintf(`; set %sCompile / unmanagedJars += Attributed.blank(file("%s"))`, sbtModule, bootstrapLibJarPath),
		fmt.Sprintf(`; %srunMain besom.bootstrap.PulumiPluginsDiscoverer --output-file %s`, sbtModule, pluginDiscovererOutputPath),
	}
	pluginsSbtCommand := strings.Join(pluginsSbtCommandParts, " ")

	return pluginsSbtCommand
}

func makeArgs(sbtModule string, cmd string) []string {
	if sbtModule != "" {
		return []string{"-batch", fmt.Sprintf("%s/%s", sbtModule, cmd)}
	}
	return []string{"-batch", cmd}
}
