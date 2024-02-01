// Copyright 2024, Pulumi Corporation.  All rights reserved.

package executors

import (
	"strings"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
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
	executor, err := m.newMavenExecutor(cmd, opts.BootstrapLibJarPath)
	if err != nil {
		return nil, err
	}

	logging.V(3).Infof(`Detected Maven executor:
            Cmd:        %s
            Dir:        %s
            RunArgs:    %s
            PluginArgs: %s
            BuildArgs:  %s
            VersionArgs %s`,
		executor.Cmd,
		executor.Dir,
		strings.Join(executor.RunArgs, " "),
		strings.Join(executor.PluginArgs, " "),
		strings.Join(executor.BuildArgs, " "),
		strings.Join(executor.VersionArgs, " "),
	)
	return executor, err
}

func (maven) isMavenProject(opts ScalaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "mvn") {
		return true, nil
	}
	return fsys.FileExists(opts.WD, "pom.xml")
}

func (maven) newMavenExecutor(cmd string, bootstrapLibJarPath string) (*ScalaExecutor, error) {
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
			/* move normal output to STDERR, because we need STDOUT for JSON with plugin results */
			"-Dorg.slf4j.simpleLogger.logFile=System.err",
			"--no-transfer-progress",
			"-DbesomBootstrapJar=" + bootstrapLibJarPath,
			"-DmainClass=besom.bootstrap.PulumiPluginsDiscoverer",
			"scala:run",
		},
		VersionArgs: []string{"--version"},
	}, nil
}
