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

// LookWrapperOrPath resolves the wrapper script of a build tool, such as gradlew, when dir holds one and
// the command on the $PATH otherwise. Only build tools with an established wrapper convention should use
// it; every other command is looked up on the $PATH alone.
//
// A wrapper that is present but cannot be executed is an error rather than a reason to fall back to the
// $PATH, which likely holds a different version of the tool than the one the project pins.
func LookWrapperOrPath(dir ParentFS, wrapper string, command string) (string, error) {
	for _, file := range wrapperFiles(dir.GOOS(), wrapper) {
		present, err := FileExists(dir, file)
		if err != nil {
			return "", err
		}
		if !present {
			continue
		}
		pathExec, err := dir.LookPath("./" + file)
		if err != nil {
			return "", fmt.Errorf("found %s in %s but cannot execute it, make it executable "+
				"or remove it to use %s from the $PATH: %w", file, dir.Path(), command, err)
		}
		return pathExec, nil
	}
	return LookPath(dir, command)
}

// wrapperFiles lists the files that make up a wrapper on goos. Build tools ship a shell script for Unix
// and a batch file for Windows side by side, and the shell script is of no use on Windows.
func wrapperFiles(goos string, wrapper string) []string {
	if goos == "windows" {
		return []string{wrapper + ".bat", wrapper + ".cmd"}
	}
	return []string{wrapper}
}
