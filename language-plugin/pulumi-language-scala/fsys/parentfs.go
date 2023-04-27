// Copyright 2022, Pulumi Corporation.  All rights reserved.

package fsys

import (
	"io/fs"
)

// File system and os interface. Helps test executor detection.
// Extends fs.FS with ability to explore parent folders, search for
// executables in PATH.
type ParentFS interface {
	fs.FS

	// Full path for the current dir.
	Path() string

	// Base path for the current dir (similar to path.Base(this.Path()))
	Base() string

	// True unless the current dir is a root dir.
	HasParent() bool

	// Resolves parent dir if HasParent() == true
	Parent() ParentFS

	// Like LookPath from os/exec but local executable paths are
	// interpreted relative to current FS.
	LookPath(string) (string, error)
}
