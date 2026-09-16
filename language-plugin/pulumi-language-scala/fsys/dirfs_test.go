package fsys

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func executableIn(t *testing.T, dir string, name string) string {
	t.Helper()
	path := filepath.Join(dir, name)
	require.NoError(t, os.WriteFile(path, []byte("#!/bin/sh\n"), 0755))
	return path
}

// An absolute path is used as is and not joined onto the project directory.
func TestDirFSLookPathAbsolute(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("relies on the Unix executable bit")
	}
	tool := executableIn(t, t.TempDir(), "tool")

	cmd, err := DirFS(t.TempDir()).LookPath(tool)
	require.NoError(t, err)
	assert.Equal(t, tool, cmd)
}

func TestDirFSLookPathRelativeToProject(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("relies on the Unix executable bit")
	}
	project := t.TempDir()
	tool := executableIn(t, project, "tool")

	cmd, err := DirFS(project).LookPath("./tool")
	require.NoError(t, err)
	assert.Equal(t, tool, cmd)
}
