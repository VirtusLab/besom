// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"fmt"
	"os"
	"io"
	"path"
	"path/filepath"
	"strings"

	"github.com/virtuslab/besom/language-host/fsys"
)

type sbt struct{}

var _ scalaExecutorFactory = &sbt{}

func (s sbt) NewScalaExecutor(opts ScalaExecutorOptions) (*ScalaExecutor, error) {
	ok, err := s.isSbtProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./sbt", "sbt"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	return s.newSbtExecutor(cmd, opts.WD, opts.LanguagePluginHomeDir)
}

func (sbt) isSbtProject(opts ScalaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "sbt") {
		return true, nil
	}
	sbtMarkers := []string{
		"project/build.properties",
		"build.sbt",
	}
	for _, p := range sbtMarkers {
		isSbt, err := fsys.FileExists(opts.WD, p)
		if err != nil {
			return false, err
		}
		if isSbt {
			return true, nil
		}
	}
	return false, nil
}

func (sbt) newSbtExecutor(cmd string, projectRoot fsys.ParentFS, languagePluginHomeDir string) (*ScalaExecutor, error) {
	sbtModule := os.Getenv("BESOM_SBT_MODULE")
	sbtBesomPluginPath := SbtBesomPluginPath(languagePluginHomeDir)
	sbtBesomPluginTargetPath := path.Join(projectRoot.Path(), "project", "BesomPlugin.scala")
	bootstrapLibJarPath := ResolveBootstrapLibJarPath(languagePluginHomeDir)
	pluginDiscovererOutputPath := PluginDiscovererOutputFilePath(projectRoot)

	se := &ScalaExecutor{
		Name:        "sbt",
		Cmd:         cmd,
		RunArgs:     makeArgs(sbtModule, "run"),
		BuildArgs:   makeArgs(sbtModule, "compile"),
		PluginArgs:  makeArgs(sbtModule, fmt.Sprintf(`besomExtractPulumiPlugins %s %s`, bootstrapLibJarPath, pluginDiscovererOutputPath)),
		VersionArgs: []string{"--numeric-version"},
		SetupProject: func() error {
			return copyFileIfNotPresent(sbtBesomPluginPath, sbtBesomPluginTargetPath)
		},
	}

	return se, nil
}

func makeArgs(sbtModule string, cmd string) []string {
	if sbtModule != "" {
		return []string{"-batch", fmt.Sprintf("%s/%s", sbtModule, cmd)}
	}
	return []string{"-batch", cmd}
}


// copyFileIfNotPresent copies a file from src to dest only if dest does not already exist.
// It uses io.Copy and preserves the source file's permissions.
// It also creates parent directories for destPath if they do not exist.
func copyFileIfNotPresent(srcPath, destPath string) error {
    // Check if destination file exists
    if _, err := os.Stat(destPath); err == nil {
        // File already exists
        return nil
    } else if !os.IsNotExist(err) {
        // Some other error occurred when checking
        return fmt.Errorf("error checking destination file: %w", err)
    }

    // Ensure parent directories exist
    destDir := filepath.Dir(destPath)
    if err := os.MkdirAll(destDir, 0755); err != nil {
        return fmt.Errorf("failed to create parent directories: %w", err)
    }

    // Open source file
    srcFile, err := os.Open(srcPath)
    if err != nil {
        return fmt.Errorf("failed to open source file: %w", err)
    }
    defer srcFile.Close()

    // Create destination file
    destFile, err := os.Create(destPath)
    if err != nil {
        return fmt.Errorf("failed to create destination file: %w", err)
    }
    defer func() {
        cerr := destFile.Close()
        if err == nil {
            err = cerr
        }
    }()

    // Copy contents
    if _, err := io.Copy(destFile, srcFile); err != nil {
        return fmt.Errorf("failed to copy file contents: %w", err)
    }

    // Copy file permissions
    srcInfo, err := os.Stat(srcPath)
    if err != nil {
        return fmt.Errorf("failed to get source file info: %w", err)
    }
    if err := os.Chmod(destPath, srcInfo.Mode()); err != nil {
        return fmt.Errorf("failed to set destination file permissions: %w", err)
    }

    return nil
}
