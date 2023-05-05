// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"fmt"
	"os/exec"
	"strings"
	"syscall"
)

// Like `cmd.Run()` but issues friendlier errors.
func runCommand(cmd *exec.Cmd) error {
	name := cmd.Path
	commandStr := strings.Join(cmd.Args, " ")
	if err := cmd.Run(); err != nil {
		if exiterr, ok := err.(*exec.ExitError); ok {
			// If the program ran, but exited with a non-zero error code.
			// This will happen often, since user errors will trigger this.
			// So, the error message should look as nice as possible.
			if status, stok := exiterr.Sys().(syscall.WaitStatus); stok {
				code := status.ExitStatus()
				return fmt.Errorf("'%v %v' exited with non-zero exit code: %d", name, commandStr, code)
			}
			return fmt.Errorf("'%v %v' exited unexpectedly: %w", name, commandStr, exiterr)
		}

		// Otherwise, we didn't even get to run the program.
		// This ought to never happen unless there's a bug or
		// system condition that prevented us from running the
		// language exec. Issue a scarier error.
		return fmt.Errorf("Problem executing '%v %v': %w", name, commandStr, err)
	}
	return nil
}
