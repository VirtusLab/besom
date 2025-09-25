// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
  "fmt"
  "path"
  "strings"
  "path/filepath"

  "github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
  "github.com/virtuslab/besom/language-host/fsys"
)

// ScalaExecutor abstracts interactions with a Scala project, ability to build, run
// Scala code, and detect plugin dependencies.
type ScalaExecutor struct {
  // User-friendly name of the executor.
  Name string

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

  // Command to print the version of the command.
  VersionArgs []string

  SetupProject func() error
}

// ScalaExecutorOptions contains information used to pick an executor.
type ScalaExecutorOptions struct {
  // Current working directory. Abstract to enable testing.
  WD fsys.ParentFS

  // The absolute path to the directory where the language plugin is installed.
  // This is used to locate the bootstrap library jar and other resources.
  LanguagePluginHomeDir string

  // The value of `runtime.options.binary` setting from
  // `Pulumi.yaml`. Optional.
  Binary string

  // The value of `runtime.options.use-executor` setting from
  // `Pulumi.yaml`. Optional.
  UseExecutor string
}

type scalaExecutorFactory interface {
  // NewScalaExecutor tries configuring an executor from the given options.
  // May return nil if options are not-applicable.
  NewScalaExecutor(ScalaExecutorOptions) (*ScalaExecutor, error)
}

func NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
  e, err := combineScalaExecutorFactories(
    &jarexec{},
    &sbt{},
    &gradle{},
    &maven{},
    &scalacli{},
  ).NewScalaExecutor(opts)
  if err != nil {
    return nil, err
  }
  if e == nil {
    return nil, fmt.Errorf("failed to configure executor, tried: jar, sbt, scala-cli, gradle, maven")
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
    logDetectedExecutor(e)
    if e != nil {
      return e, nil
    }
  }
  return nil, nil
}

func combineScalaExecutorFactories(variations ...scalaExecutorFactory) scalaExecutorFactory {
  return combinedScalaExecutorFactory(variations)
}

const PluginDiscovererOutputFileName = ".besom-pulumi-plugins.json"

func PluginDiscovererOutputFilePath(projectRoot fsys.ParentFS) string {
  return path.Join(projectRoot.Path(), PluginDiscovererOutputFileName)
}

func ResolveBootstrapLibJarPath(languagePluginHomeDir string) string {
  return filepath.Join(languagePluginHomeDir, "bootstrap.jar")
}

func SbtBesomPluginPath(languagePluginHomeDir string) string {
  return filepath.Join(languagePluginHomeDir, "BesomPlugin.scala")
}

func logDetectedExecutor(executor *ScalaExecutor) {
  if executor == nil {
    return
  }

  logging.V(5).Infof(`Detected Scala executor:
      Name:       %s
      Cmd:        %s
      Dir:        %s
      RunArgs:    %s
      PluginArgs: %s
      BuildArgs:  %s
      VersionArgs: %s`,
    executor.Name,
    executor.Cmd,
    executor.Dir,
    strings.Join(executor.RunArgs, " "),
    strings.Join(executor.PluginArgs, " "),
    strings.Join(executor.BuildArgs, " "),
    strings.Join(executor.VersionArgs, " "),
  )
}
