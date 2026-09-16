// Copyright 2022, Pulumi Corporation.  All rights reserved.

package fsys

import (
	"io/fs"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
)

// Use the real OS.
func DirFS(dir string) ParentFS {
	return &osDirFS{dir}
}

type osDirFS struct {
	dir string
}

var _ ParentFS = &osDirFS{}

var _ fs.StatFS = &osDirFS{}

func (o osDirFS) fs() fs.FS {
	return os.DirFS(o.dir)
}

func (o osDirFS) Path() string {
	return o.dir
}

func (o osDirFS) Base() string {
	return filepath.Base(o.dir)
}

func (o osDirFS) Open(name string) (fs.File, error) {
	return o.fs().Open(name)
}

func (o osDirFS) Stat(name string) (fs.FileInfo, error) {
	return fs.Stat(o.fs(), name)
}

func (o osDirFS) HasParent() bool {
	pDir := filepath.Dir(o.dir)
	return pDir != o.dir
}

func (o osDirFS) Parent() ParentFS {
	pDir := filepath.Dir(o.dir)
	return &osDirFS{pDir}
}

func (o osDirFS) LookPath(exe string) (string, error) {
	if filepath.IsAbs(exe) {
		return exec.LookPath(exe)
	}
	// Windows users write relative paths with either separator.
	if strings.ContainsAny(exe, "/"+string(filepath.Separator)) {
		return exec.LookPath(filepath.Join(o.dir, exe))
	}
	return exec.LookPath(exe)
}

func (o osDirFS) GOOS() string {
	return runtime.GOOS
}
