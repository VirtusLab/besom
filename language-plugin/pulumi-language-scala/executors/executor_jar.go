// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"path/filepath"
	"strings"

	"github.com/virtuslab/besom/language-host/fsys"
)

type jarexec struct{}

var _ scalaExecutorFactory = &jarexec{}

func (j jarexec) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	if opts.Binary == "" {
		return nil, nil
	}
	suffix := strings.ToLower(filepath.Ext(opts.Binary))
	if suffix != ".jar" {
		return nil, nil
	}
	cmd, err := fsys.LookPath(opts.WD, "java")
	if err != nil {
		return nil, err
	}
	bootstrapLibJarPath := ResolveBootstrapLibJarPath(opts.LanguagePluginHomeDir)
	pluginDiscovererOutputPath := PluginDiscovererOutputFilePath(opts.WD)
	return j.newJarExecutor(cmd, bootstrapLibJarPath, opts.Binary, pluginDiscovererOutputPath)
}

func (jarexec) newJarExecutor(cmd string, bootstrapLibJarPath string, rawBinaryPath string, pluginDiscovererOutputPath string) (*ScalaExecutor, error) {
	binaryPath := filepath.Clean(rawBinaryPath)
	classPath := bootstrapLibJarPath + ":" + binaryPath

	return &ScalaExecutor{
		Name:        "jar",
		Cmd:         cmd,
		BuildArgs:   nil, // not supported
		RunArgs:     []string{"-jar", binaryPath},
		PluginArgs:  []string{"-cp", classPath, "besom.bootstrap.PulumiPluginsDiscoverer", "--output-file", pluginDiscovererOutputPath},
		VersionArgs: []string{"-version"},
		SetupProject: func() error { return nil }, // NOOP
	}, nil
}
