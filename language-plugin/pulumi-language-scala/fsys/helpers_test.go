package fsys

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// wrapperEnv fakes a project directory and a $PATH holding the tool, both on the real file system since
// the executable bit is what is being tested.
func wrapperEnv(t *testing.T) (projectDir string, toolOnPath string) {
	t.Helper()
	if runtime.GOOS == "windows" {
		t.Skip("relies on the Unix executable bit")
	}
	projectDir = t.TempDir()
	binDir := t.TempDir()
	toolOnPath = filepath.Join(binDir, "tool")
	require.NoError(t, os.WriteFile(toolOnPath, []byte("#!/bin/sh\n"), 0755))
	t.Setenv("PATH", binDir)
	return projectDir, toolOnPath
}

func TestLookWrapperOrPathPrefersWrapper(t *testing.T) {
	projectDir, _ := wrapperEnv(t)
	wrapper := filepath.Join(projectDir, "toolw")
	require.NoError(t, os.WriteFile(wrapper, []byte("#!/bin/sh\n"), 0755))

	cmd, err := LookWrapperOrPath(DirFS(projectDir), "toolw", "tool")
	require.NoError(t, err)
	assert.Equal(t, wrapper, cmd)
}

func TestLookWrapperOrPathFallsBackToPathWithoutWrapper(t *testing.T) {
	projectDir, toolOnPath := wrapperEnv(t)

	cmd, err := LookWrapperOrPath(DirFS(projectDir), "toolw", "tool")
	require.NoError(t, err)
	assert.Equal(t, toolOnPath, cmd)
}

// Typically a wrapper committed from Windows. Falling back would run whatever version is on the $PATH.
func TestLookWrapperOrPathFailsOnNonExecutableWrapper(t *testing.T) {
	projectDir, _ := wrapperEnv(t)
	require.NoError(t, os.WriteFile(filepath.Join(projectDir, "toolw"), []byte("#!/bin/sh\n"), 0644))

	_, err := LookWrapperOrPath(DirFS(projectDir), "toolw", "tool")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "found toolw in "+projectDir+" but cannot execute it")
}

// The batch file is for Windows only and must not shadow the tool on the $PATH elsewhere.
func TestLookWrapperOrPathIgnoresWindowsWrapperOnUnix(t *testing.T) {
	projectDir, toolOnPath := wrapperEnv(t)
	require.NoError(t, os.WriteFile(filepath.Join(projectDir, "toolw.bat"), []byte("@echo off\n"), 0644))

	cmd, err := LookWrapperOrPath(DirFS(projectDir), "toolw", "tool")
	require.NoError(t, err)
	assert.Equal(t, toolOnPath, cmd)
}
