package executors

import (
	"errors"
	"testing"
	"testing/fstest"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/virtuslab/besom/language-host/fsys"
)

// scalaCliEnv isolates a test from the developer's environment and fakes the check telling the Scala CLI
// based scala launcher apart from the legacy runner: scala is a launcher unless legacyScala is set.
// Returns the commands the check was run against.
func scalaCliEnv(t *testing.T, legacyScala bool) *[]string {
	t.Helper()
	t.Setenv(ScalaCliCommandEnvVar, "")
	probed := &[]string{}
	original := probeScalaLauncher
	probeScalaLauncher = func(cmd string) error {
		*probed = append(*probed, cmd)
		if legacyScala {
			return errors.New("No such file or class on classpath: version")
		}
		return nil
	}
	t.Cleanup(func() { probeScalaLauncher = original })
	return probed
}

// A Scala CLI project has no build tool markers, so it falls through to the scala-cli executor.
func scalaCliProject(exes map[string]string) fsys.ParentFS {
	return fsys.TestFS(".", exes, fstest.MapFS{"project.scala": {}})
}

var bothScalaCommands = map[string]string{
	"scala-cli": "/usr/bin/scala-cli",
	"scala":     "/usr/bin/scala",
}

func TestScalaCliOnly(t *testing.T) {
	probed := scalaCliEnv(t, false)
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(map[string]string{"scala-cli": "/usr/bin/scala-cli"})})
	require.NoError(t, err)
	assert.Equal(t, "scala-cli", exec.Name)
	assert.Equal(t, "/usr/bin/scala-cli", exec.Cmd)
	assert.Empty(t, *probed)
}

func TestScalaLauncherOnly(t *testing.T) {
	scalaCliEnv(t, false)
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(map[string]string{"scala": "/usr/bin/scala"})})
	require.NoError(t, err)
	assert.Equal(t, "scala-cli", exec.Name)
	assert.Equal(t, "/usr/bin/scala", exec.Cmd)
	assert.Equal(t, []string{"run", ".", ""}, exec.RunArgs)
	assert.Equal(t, []string{"version", "--cli", "--offline"}, exec.VersionArgs)
}

func TestLegacyScalaRunnerOnlyFails(t *testing.T) {
	scalaCliEnv(t, true)
	_, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(map[string]string{"scala": "/usr/bin/scala"})})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "/usr/bin/scala is not the Scala CLI based launcher of Scala 3.5+")
}

func TestNeitherScalaCommandFails(t *testing.T) {
	probed := scalaCliEnv(t, false)
	_, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(map[string]string{})})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "could not find scala-cli nor the scala launcher of Scala 3.5+")
	assert.Empty(t, *probed)
}

func TestBothScalaCommandsFailWithoutAChoice(t *testing.T) {
	scalaCliEnv(t, false)
	_, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(bothScalaCommands)})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "found both scala-cli (/usr/bin/scala-cli) and scala (/usr/bin/scala)")
	assert.Contains(t, err.Error(), "runtime.options.use-executor")
	assert.Contains(t, err.Error(), ScalaCliCommandEnvVar)
}

// Having an old Scala installed next to scala-cli is common and is not an ambiguity.
func TestLegacyScalaRunnerNextToScalaCliIsIgnored(t *testing.T) {
	scalaCliEnv(t, true)
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(bothScalaCommands)})
	require.NoError(t, err)
	assert.Equal(t, "/usr/bin/scala-cli", exec.Cmd)
}

// Only a chosen scala command is checked, scala-cli is Scala CLI by definition.
func expectedProbes(chosen string) []string {
	if chosen == "scala" {
		return []string{bothScalaCommands["scala"]}
	}
	return []string{}
}

func TestBothScalaCommandsChosenByEnvVar(t *testing.T) {
	for _, chosen := range []string{"scala", "scala-cli"} {
		t.Run(chosen, func(t *testing.T) {
			probed := scalaCliEnv(t, false)
			t.Setenv(ScalaCliCommandEnvVar, chosen)
			exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(bothScalaCommands)})
			require.NoError(t, err)
			assert.Equal(t, bothScalaCommands[chosen], exec.Cmd)
			assert.Equal(t, expectedProbes(chosen), *probed)
		})
	}
}

func TestBothScalaCommandsChosenByUseExecutor(t *testing.T) {
	for _, chosen := range []string{"scala", "scala-cli"} {
		t.Run(chosen, func(t *testing.T) {
			probed := scalaCliEnv(t, false)
			exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(bothScalaCommands), UseExecutor: chosen})
			require.NoError(t, err)
			assert.Equal(t, bothScalaCommands[chosen], exec.Cmd)
			assert.Equal(t, expectedProbes(chosen), *probed)
		})
	}
}

// An explicit choice cannot make a legacy runner work, so it fails right away instead of on the first
// scala-cli subcommand.
func TestLegacyScalaRunnerChosenExplicitlyFails(t *testing.T) {
	legacyScala := map[string]string{"/opt/scala-2.13/bin/scala": "/opt/scala-2.13/bin/scala", "scala": "/usr/bin/scala"}
	for name, chosen := range map[string]ScalaExecutorOptions{
		"use-executor by name": {UseExecutor: "scala"},
		"use-executor by path": {UseExecutor: "/opt/scala-2.13/bin/scala"},
		"env var":              {},
	} {
		t.Run(name, func(t *testing.T) {
			scalaCliEnv(t, true)
			if chosen.UseExecutor == "" {
				t.Setenv(ScalaCliCommandEnvVar, "scala")
			}
			chosen.WD = scalaCliProject(legacyScala)
			_, err := NewScalaExecutor(chosen)
			require.Error(t, err)
			assert.Contains(t, err.Error(), "is not the Scala CLI based launcher of Scala 3.5+")
		})
	}
}

func TestIsScalaCommand(t *testing.T) {
	for cmd, expected := range map[string]bool{
		"/usr/bin/scala":      true,
		"./scala":             true,
		"scala.bat":           true,
		"Scala.exe":           true,
		"/usr/bin/scala-cli":  false,
		"/opt/scala/bin/java": false,
		"scalac":              false,
	} {
		assert.Equal(t, expected, isScalaCommand(cmd), cmd)
	}
}

// Pulumi.yaml describes the project, the environment variable only the machine it runs on.
func TestUseExecutorTakesPrecedenceOverEnvVar(t *testing.T) {
	scalaCliEnv(t, false)
	t.Setenv(ScalaCliCommandEnvVar, "scala-cli")
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(bothScalaCommands), UseExecutor: "scala"})
	require.NoError(t, err)
	assert.Equal(t, "/usr/bin/scala", exec.Cmd)
}

func TestEnvVarChoosingMissingCommandFails(t *testing.T) {
	scalaCliEnv(t, false)
	t.Setenv(ScalaCliCommandEnvVar, "scala")
	_, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(map[string]string{"scala-cli": "/usr/bin/scala-cli"})})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "could not find scala")
}

// Only the $PATH is searched, files named like the commands in the project directory are not.
func TestProjectLocalScalaFilesAreIgnored(t *testing.T) {
	probed := scalaCliEnv(t, false)
	fs := fsys.TestFS(".", map[string]string{"scala-cli": "/usr/bin/scala-cli"}, fstest.MapFS{
		"project.scala": {},
		"scala":         {},
		"scala-cli":     {},
	})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	require.NoError(t, err)
	assert.Equal(t, "/usr/bin/scala-cli", exec.Cmd)
	assert.Empty(t, *probed)
}
