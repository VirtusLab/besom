// Copyright 2022, Pulumi Corporation.  All rights reserved.

package fsys

import (
	"fmt"
	"io/fs"
	"path"
	"strings"
	"testing/fstest"
)

// Implements parentFS for testing. Unlike the prod implementation,
// path operations do not vary by OS but are unix-y.
//
// files is an in-memory filesystem
//
// workdir is a relative path to the current working dir in the
// in-memory filesystem
//
// globalPath maps short executable names to full paths to emulate
// PATH resolution in LookPath.
func TestFS(
	workdir string,
	globalPath map[string]string,
	files fstest.MapFS,
) ParentFS {
	return &testFS{workdir, globalPath, files}
}

type testFS struct {
	path  string
	exes  map[string]string
	mapfs fstest.MapFS
}

var _ ParentFS = &testFS{}

func (t testFS) fs() fs.FS {
	sub, err := fs.Sub(t.mapfs, t.path)
	if err != nil {
		panic(err)
	}
	return sub
}

func (t testFS) Path() string {
	return t.path
}

func (t testFS) Base() string {
	return path.Base(t.path)
}

func (t testFS) Open(name string) (fs.File, error) {
	return t.fs().Open(name)
}

func (t testFS) Stat(name string) (fs.FileInfo, error) {
	return fs.Stat(t.fs(), name)
}

func (t testFS) HasParent() bool {
	return path.Dir(t.path) != t.path
}

func (t testFS) Parent() ParentFS {
	return testFS{path.Dir(t.path), t.exes, t.mapfs}
}

func (t testFS) LookPath(exe string) (string, error) {
	if strings.Contains(exe, "/") {
		ok, err := FileExists(t.fs(), path.Join(".", exe))
		if err != nil {
			return "", err
		}
		if ok {
			return "./" + path.Join(t.Path(), exe), nil
		}
	}
	if found, ok := t.exes[exe]; ok {
		return found, nil
	}
	return "", fmt.Errorf("Not found: %v", exe)
}
