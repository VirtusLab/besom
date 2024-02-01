// Copyright 2024, Pulumi Corporation.  All rights reserved.

package executors

import (
	"fmt"
	"io/fs"
	"strings"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
	"github.com/virtuslab/besom/language-host/fsys"
)

type gradle struct{}

var _ scalaExecutorFactory = &gradle{}

func (g gradle) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	ok, err := g.isGradleProject(opts.WD, opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./gradlew", "gradle"}
	}
	gradleRoot, subproject, err := g.findGradleRoot(opts.WD)
	if err != nil {
		return nil, err
	}
	cmd, err := fsys.LookPath(gradleRoot, probePaths...)
	if err != nil {
		return nil, err
	}
	executor, err := g.newGradleExecutor(gradleRoot, cmd, subproject, opts.BootstrapLibJarPath)
	if err != nil {
		return nil, err
	}

	logging.V(3).Infof(`Detected Gradle executor:
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

func (gradle) findGradleRoot(workdir fsys.ParentFS) (fsys.ParentFS, string, error) {
	gradleRootMarkers := []string{
		"settings.gradle",
		"settings.gradle.kts",
	}
	d := workdir
	subproject := ""
	for {
		for _, p := range gradleRootMarkers {
			isGradleRoot, err := fsys.FileExists(d, p)
			if err != nil {
				return nil, subproject, err
			}
			if isGradleRoot {
				return d, subproject, nil
			}
		}
		if !d.HasParent() {
			// Abort search and assume workdir is the root
			return workdir, subproject, nil
		}
		subproject = fmt.Sprintf(":%s%s", d.Base(), subproject)
		d = d.Parent()
	}
}

func (gradle) isGradleProject(dir fs.FS, opts ScalaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "gradle") {
		return true, nil
	}
	gradleMarkers := []string{
		"settings.gradle",
		"settings.gradle.kts",
		"build.gradle",
	}
	for _, p := range gradleMarkers {
		isGradle, err := fsys.FileExists(dir, p)
		if err != nil {
			return false, err
		}
		if isGradle {
			return true, nil
		}
	}
	return false, nil
}

func (g gradle) newGradleExecutor(gradleRoot fsys.ParentFS, cmd, subproject string, bootstrapLibJarPath string) (*ScalaExecutor, error) {
	se := &ScalaExecutor{
		Name:      "gradle",
		Cmd:       cmd,
		Dir:       gradleRoot.Path(),
		BuildArgs: []string{g.prefix(subproject, "build"), "--console=plain"},
		RunArgs:   []string{g.prefix(subproject, "run"), "--console=plain"},
		PluginArgs: []string{
			/* STDOUT needs to be clean of gradle output,
			   because we expect a JSON with plugin
			   results */
			"-q", // must go first due to a bug https://github.com/gradle/gradle/issues/5098
			g.prefix(subproject, "run"), "--console=plain",
			"-PbesomBootstrapJar=" + bootstrapLibJarPath,
			"-PmainClass=besom.bootstrap.PulumiPluginsDiscoverer",
		},
		VersionArgs: []string{"--version"},
	}

	return se, nil
}

func (gradle) prefix(subproject, task string) string {
	if subproject == "" {
		return task
	}
	return fmt.Sprintf("%s:%s", subproject, task)
}
