// Copyright 2024, Pulumi Corporation.  All rights reserved.

package executors

import (
	"strings"

	"github.com/virtuslab/besom/language-host/fsys"
)

type maven struct{}

var _ scalaExecutorFactory = &maven{}

func (m maven) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	ok, err := m.isMavenProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./mvnw", "mvn"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	bootstrapLibJarPath := ResolveBootstrapLibJarPath(opts.LanguagePluginHomeDir)
	pluginDiscovererOutputPath := PluginDiscovererOutputFilePath(opts.WD)
	executor, err := m.newMavenExecutor(cmd, bootstrapLibJarPath, pluginDiscovererOutputPath)
	if err != nil {
		return nil, err
	}

	return executor, err
}

func (maven) isMavenProject(opts ScalaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "mvn") {
		return true, nil
	}
	return fsys.FileExists(opts.WD, "pom.xml")
}

func (maven) newMavenExecutor(cmd string, bootstrapLibJarPath string, pluginDiscovererOutputPath string) (*ScalaExecutor, error) {
	return &ScalaExecutor{
		Name: "maven",
		Cmd:  cmd,
		BuildArgs: []string{
			/* only output warning or higher to reduce noise */
			"-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
			"--no-transfer-progress",
			"scala:compile",
		},
		RunArgs: []string{
			/* only output warning or higher to reduce noise */
			"-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
			"--no-transfer-progress",
			"scala:run",
		},
		PluginArgs: []string{
			"--no-transfer-progress",
			"-DbesomBootstrapJar=" + bootstrapLibJarPath,
			"-DmainClass=besom.bootstrap.PulumiPluginsDiscoverer",
			"scala:run",
			"-DaddArgs=--output-file|" + pluginDiscovererOutputPath,
		},
		VersionArgs: []string{"--version"},
		SetupProject: func() error { return nil }, // NOOP
	}, nil
}
