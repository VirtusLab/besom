// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
	"github.com/virtuslab/besom/language-host/fsys"
)

type scalacli struct{}

var _ scalaExecutorFactory = &scalacli{}

// ScalaCliCommandEnvVar names the command the scala-cli executor runs: `scala-cli`, `scala` or a path to
// either. It settles the choice when both commands are installed. `runtime.options.use-executor` in
// Pulumi.yaml takes precedence over it.
const ScalaCliCommandEnvVar = "BESOM_LANGHOST_SCALA_CLI_COMMAND"

var scalaCliVersionArgs = []string{"version", "--cli", "--offline"}

// probeScalaLauncher fails unless cmd is the Scala CLI based `scala` launcher shipped with Scala 3.5+.
// The runner of Scala 2 and of older Scala 3 releases is also called `scala`, but it does not understand
// scala-cli subcommands. A variable so that tests do not have to spawn processes.
var probeScalaLauncher = func(cmd string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	output, err := exec.CommandContext(ctx, cmd, scalaCliVersionArgs...).CombinedOutput()
	if err != nil {
		return fmt.Errorf("'%s %s' failed: %w: %s", cmd, strings.Join(scalaCliVersionArgs, " "), err, strings.TrimSpace(string(output)))
	}
	return nil
}

func (s scalacli) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	cmd, err := s.resolveCommand(opts)
	if err != nil {
		return nil, err
	}
	bootstrapLibJarPath := ResolveBootstrapLibJarPath(opts.LanguagePluginHomeDir)
	pluginDiscovererOutputPath := PluginDiscovererOutputFilePath(opts.WD)
	return s.newScalaCliExecutor(cmd, bootstrapLibJarPath, pluginDiscovererOutputPath)
}

// resolveCommand picks between `scala-cli` and the `scala` launcher of Scala 3.5+, which is Scala CLI under
// another name. Neither is preferred over the other: when both are installed the user has to choose, as
// they may well be different Scala CLI versions. Scala CLI has no wrapper script convention, so unlike the
// build tool executors this one never picks a command up from the project directory.
func (scalacli) resolveCommand(opts ScalaExecutorOptions) (string, error) {
	if opts.UseExecutor != "" {
		return resolveChosenCommand(opts.WD, opts.UseExecutor)
	}
	if chosen := os.Getenv(ScalaCliCommandEnvVar); chosen != "" {
		return resolveChosenCommand(opts.WD, chosen)
	}

	scalaCliCmd, scalaCliErr := fsys.LookPath(opts.WD, "scala-cli")
	scalaCmd, scalaErr := fsys.LookPath(opts.WD, "scala")
	if scalaErr == nil {
		if err := checkScalaLauncher(scalaCmd); err != nil {
			logging.V(5).Infof("Ignoring %s: %s", scalaCmd, err)
			scalaErr = err
		}
	}

	switch {
	case scalaCliErr == nil && scalaErr == nil:
		return "", fmt.Errorf(
			"found both scala-cli (%s) and scala (%s), refusing to guess which one to use; "+
				"choose one by setting `runtime.options.use-executor` in Pulumi.yaml "+
				"or the %s environment variable to `scala-cli` or `scala`",
			scalaCliCmd, scalaCmd, ScalaCliCommandEnvVar)
	case scalaCliErr == nil:
		return scalaCliCmd, nil
	case scalaErr == nil:
		return scalaCmd, nil
	default:
		return "", fmt.Errorf("could not find scala-cli nor the scala launcher of Scala 3.5+: %w; %w", scalaCliErr, scalaErr)
	}
}

// resolveChosenCommand resolves a command the user picked explicitly. A `scala` command, by name or by
// path, still has to be the Scala CLI based launcher: the legacy runner would only fail later, on the first
// scala-cli subcommand, with an error that does not point at the cause.
func resolveChosenCommand(wd fsys.ParentFS, chosen string) (string, error) {
	cmd, err := fsys.LookPath(wd, chosen)
	if err != nil {
		return "", err
	}
	if isScalaCommand(cmd) {
		if err := checkScalaLauncher(cmd); err != nil {
			return "", fmt.Errorf("%w; use scala-cli or the scala command of Scala 3.5+ instead", err)
		}
	}
	return cmd, nil
}

// isScalaCommand tells whether cmd names a `scala` command, e.g. /usr/bin/scala or scala.bat.
func isScalaCommand(cmd string) bool {
	name := strings.ToLower(filepath.Base(cmd))
	return strings.TrimSuffix(name, filepath.Ext(name)) == "scala"
}

func checkScalaLauncher(cmd string) error {
	if err := probeScalaLauncher(cmd); err != nil {
		return fmt.Errorf("%s is not the Scala CLI based launcher of Scala 3.5+: %w", cmd, err)
	}
	return nil
}

func (scalacli) newScalaCliExecutor(cmd string, bootstrapLibJarPath string, pluginDiscovererOutputPath string) (*ScalaExecutor, error) {
	scalaCliOpts := os.Getenv("BESOM_LANGHOST_SCALA_CLI_OPTS")
	return &ScalaExecutor{
		Name:        "scala-cli",
		Cmd:         cmd,
		BuildArgs:   []string{"compile", ".", scalaCliOpts},
		RunArgs:     []string{"run", ".", scalaCliOpts},
		PluginArgs:  []string{"run", ".", scalaCliOpts, "--jar", bootstrapLibJarPath, "--main-class", "besom.bootstrap.PulumiPluginsDiscoverer", "--", "--output-file", pluginDiscovererOutputPath},
		VersionArgs: scalaCliVersionArgs,
		SetupProject: func() error { return nil }, // NOOP
	}, nil
}
