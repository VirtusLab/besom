package besom.integration.codegen

import os./
import besom.integration.common.*
import munit.{Slow, TestOptions}

import scala.concurrent.duration.Duration

//noinspection ScalaWeakerAccess,TypeAnnotation,ScalaFileName
class CodegenTests extends munit.FunSuite {
  val isSlowExclude = false
  override def munitTests(): Seq[Test] = super
    .munitTests()
    .filterNot(tagsWhen(isCI)(LocalOnly))
    .filterNot(
      tagsWhen(isCI || isSlowExclude)(Slow)
    )

  override val munitTimeout = Duration(20, "min")
  val testdata              = os.pwd / "integration-tests" / "resources" / "testdata"

  def codegen(
    schemaPath: os.Path,
    codegenOutputDir: os.Path,
    schemaName: String,
    schemaVersion: String,
    besomVersion: String
  ): os.proc =
    pproc(
      "scala-cli",
      "run",
      "codegen",
      "--suppress-experimental-feature-warning",
      "--suppress-directives-in-multiple-files-warning",
      "--",
      "test",
      schemaPath,
      codegenOutputDir,
      schemaName,
      schemaVersion,
      besomVersion
    )

  val slowDirList = List(
    "akamai"
  )

  val blockDirList = List(
    "secrets", // FIXME: broken
    "simple-plain-schema", // FIXME: broken
    "simple-plain-schema-with-root-package", // FIXME: broken
    "simple-enum-schema", // FIXME: broken
    "simple-resource-schema", // FIXME: broken
    "simple-methods-schema", // FIXME: broken
    "simple-methods-schema-single-value-returns", // FIXME: broken
    "simple-yaml-schema", // FIXME: broken
    "simplified-invokes", // FIXME: broken
    "nested-module", // FIXME: broken
    "nested-module-thirdparty", // FIXME: broken
    "external-resource-schema", // FIXME: broken
    "external-enum", // FIXME: broken
    "enum-reference", // FIXME: broken
    "different-enum", // FIXME: broken
    "embedded-crd-types", // FIXME: broken
    "hyphen-url", // FIXME: broken
    "naming-collisions", // FIXME: broken
    "mini-azurenative", // FIXME: broken
    "mini-awsnative", // FIXME: broken
    "mini-awsclassic", // FIXME: broken
    "jumbo-resources", // FIXME: broken
    "replace-on-change", // FIXME: broken
    "resource-property-overlap", // FIXME: broken
    "cyclic-types", // FIXME: broken
    "plain-and-default", // FIXME: broken
    "different-package-name-conflict", // FIXME: broken
    "azure-native-nested-types" // FIXME: broken
  ).appendedAll(slowDirList)

  val blockFileList = List()

  val schemas =
    for schema <- os
        .walk(testdata)
        .filter(os.isFile(_))
        .filter(f => List("json", "yaml").contains(f.ext))
        .filter {
          case _ / d / _ if blockDirList.contains(d) => false
          case _ / f if blockFileList.contains(f)    => false
          case _                                     => true
        }
    yield schema

  case class TestData(
    name: String,
    schema: os.Path,
    outputDir: os.Path
  )

  val test = (schema: os.Path) =>
    FunFixture[TestData](
      setup = { _ =>
        val name               = (schema / os.up).last
        val outputDir: os.Path = schema / os.up / "codegen"
        TestData(name, schema, outputDir)
      },
      teardown = { data => os.remove.all(data.outputDir) }
    )

  schemas.foreach { schema =>
    test(schema).test(s"schema ${schema.relativeTo(testdata)} should codegen") { data =>
      schemas.foreach { schema =>
        val result = codegen(schema, data.outputDir, data.name, "0.0.0", coreVersion).call(check = false)
        val output = result.out.text()
        assert(output.contains("Finished generating SDK codebase"), s"Output:\n$output\n")
        assert(result.exitCode == 0)

        val compiled = scalaCli.compile(".").call(cwd = data.outputDir, check = false)
        assert(compiled.exitCode == 0)
      }
    }
  }
}
