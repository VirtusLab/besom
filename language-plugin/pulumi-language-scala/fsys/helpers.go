// Copyright 2022, Pulumi Corporation.  All rights reserved.

package fsys

import (
	"fmt"
	"io/fs"
	"os"
	"strings"
)

// Checks if a file exists.
func FileExists(dir fs.FS, path string) (bool, error) {
	_, err := fs.Stat(dir, path)
	if err != nil && os.IsNotExist(err) {
		return false, nil
	}
	return true, err
}

// Like os.LookPath but tries multiple executables left-to-right.
func LookPath(dir ParentFS, files ...string) (string, error) {
	var lastError error
	for _, file := range files {
		pathExec, err := dir.LookPath(file)
		if err == nil {
			return pathExec, nil
		}
		lastError = err
	}
	return "", fmt.Errorf("could not find %s on the $PATH: %w",
		strings.Join(files, ", "), lastError)
}
