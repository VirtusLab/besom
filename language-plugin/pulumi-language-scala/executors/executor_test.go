package executors

import (
	"testing"
	"testing/fstest"

	"github.com/stretchr/testify/assert"

	"github.com/virtuslab/besom/language-host/fsys"
)

func TestGradleSimple(t *testing.T) {
	fs := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"settings.gradle":  {},
			"app/build.gradle": {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
	assert.Equal(t, ".", exec.Dir)
	assert.Equal(t,
		[]string{":app:run", "--console=plain"},
		exec.RunArgs)
}

func TestGradleCurrentDir(t *testing.T) {
	fs := fsys.TestFS(".",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"settings.gradle": {},
			"build.gradle":    {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
	assert.Equal(t, ".", exec.Dir)
	assert.Equal(t,
		[]string{"run", "--console=plain"},
		exec.RunArgs)
}

func TestGradleKTS(t *testing.T) {
	fs := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"settings.gradle.kts": {},
			"app/build.gradle":    {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
}

func TestGradlew(t *testing.T) {
	fs := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"app/build.gradle": {},
			"gradlew":          {},
			"settings.gradle":  {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	assert.NoError(t, err)
	assert.Equal(t, "./gradlew", exec.Cmd)
}

func TestGradleMultiProject(t *testing.T) {
	fs := fsys.TestFS("services/app-cluster",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"services/app-cluster/build.gradle":  {},
			"services/mgmt-cluster/build.gradle": {},
			"gradlew":                            {},
			"settings.gradle":                    {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	assert.NoError(t, err)
	assert.Equal(t, "./gradlew", exec.Cmd)
	assert.Equal(t, ".", exec.Dir)
	assert.Equal(t,
		[]string{":services:app-cluster:run", "--console=plain"},
		exec.RunArgs)
}

func TestGradleUseExecutor(t *testing.T) {
	fs := fsys.TestFS("app",
		map[string]string{
			"gradle":             "/usr/bin/gradle",
			"/bin/custom-gradle": "/bin/custom-gradle",
		},
		fstest.MapFS{
			"gradlew":          {},
			"custom-gradlew":   {},
			"settings.gradle":  {},
			"app/build.gradle": {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{
		WD:          fs,
		UseExecutor: "./custom-gradlew",
	})
	assert.NoError(t, err)
	assert.Equal(t, "./custom-gradlew", exec.Cmd)

	exec, err = NewScalaExecutor(ScalaExecutorOptions{
		WD:          fs,
		UseExecutor: "/bin/custom-gradle",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/bin/custom-gradle", exec.Cmd)

	// Even if no marker settings.gradle files are found,
	// UseExecutor forces the use of gradle.
	fs = fsys.TestFS("app",
		map[string]string{
			"gradle": "/usr/bin/gradle",
		},
		fstest.MapFS{
			"app/hello.txt": {},
		})

	exec, err = NewScalaExecutor(ScalaExecutorOptions{
		WD:          fs,
		UseExecutor: "gradle",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
}

func TestMavenSimple(t *testing.T) {
	fs := fsys.TestFS(".",
		map[string]string{"mvn": "/usr/bin/mvn"},
		fstest.MapFS{
			"pom.xml": {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mvn", exec.Cmd)
}

func TestMavenW(t *testing.T) {
	fs := fsys.TestFS(".",
		map[string]string{"mvn": "/usr/bin/mvn"},
		fstest.MapFS{
			"mvnw":    {},
			"pom.xml": {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{WD: fs})
	assert.NoError(t, err)
	assert.Equal(t, "./mvnw", exec.Cmd)
}

func TestMavenUseExecutor(t *testing.T) {
	fs := fsys.TestFS(".",
		map[string]string{
			"mvn":             "/usr/bin/mvn",
			"/bin/custom-mvn": "/bin/custom-mvn",
		},
		fstest.MapFS{
			"mvnw":        {},
			"custom-mvnw": {},
			"pom.xml":     {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{
		WD:          fs,
		UseExecutor: "./custom-mvnw",
	})
	assert.NoError(t, err)
	assert.Equal(t, "./custom-mvnw", exec.Cmd)

	exec, err = NewScalaExecutor(ScalaExecutorOptions{
		WD:          fs,
		UseExecutor: "/bin/custom-mvn",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/bin/custom-mvn", exec.Cmd)

	// Even if no marker pom.xml files are found,
	// UseExecutor forces the use of gradle.
	fs = fsys.TestFS(".",
		map[string]string{
			"mvn": "/usr/bin/mvn",
		},
		fstest.MapFS{
			"hello.txt": {},
		})
	exec, err = NewScalaExecutor(ScalaExecutorOptions{
		WD:          fs,
		UseExecutor: "mvn",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mvn", exec.Cmd)
}

func TestJarExecSimple(t *testing.T) {
	fs := fsys.TestFS(".",
		map[string]string{"java": "/usr/bin/java"},
		fstest.MapFS{"dist/hello.jar": {}})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{
		WD:     fs,
		Binary: "dist/hello.jar",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/java", exec.Cmd)
	assert.Equal(t, []string{"-jar", "dist/hello.jar"}, exec.RunArgs)
}

func TestSBTExecutor(t *testing.T) {
	fs := fsys.TestFS(".",
		map[string]string{"sbt": "/usr/bin/sbt"},
		fstest.MapFS{
			"build.sbt": {},
		})
	exec, err := NewScalaExecutor(ScalaExecutorOptions{
		WD: fs,
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/sbt", exec.Cmd)
	assert.Equal(t, []string{"-batch", "run"}, exec.RunArgs)
}
