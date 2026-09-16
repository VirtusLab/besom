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

func TestBothScalaCommandsChosenByEnvVar(t *testing.T) {
	for _, chosen := range []string{"scala", "scala-cli"} {
		t.Run(chosen, func(t *testing.T) {
			probed := scalaCliEnv(t, false)
			t.Setenv(ScalaCliCommandEnvVar, chosen)
			exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: scalaCliProject(bothScalaCommands)})
			require.NoError(t, err)
			assert.Equal(t, bothScalaCommands[chosen], exec.Cmd)
			assert.Empty(t, *probed)
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
			assert.Empty(t, *probed)
		})
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
