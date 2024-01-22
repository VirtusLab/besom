package besom.scripts

import os.*

import scala.annotation.tailrec

object Schemas:
  val cwd = besomDir

  def main(args: String*): Unit =
    args match
      case "fetch" :: Nil => fetchSchemas(cwd)
      case "all" :: Nil =>
        fetchSchemas(cwd)
        println("fetched")
      case other =>
        println(s"unknown command: $other")
        sys.exit(1)

  def fetchSchemas(cwd: os.Path): Unit =
    val pulumiRepoPath     = cwd / "target" / "pulumi-codegen-testdata"
    val pulumiJavaRepoPath = cwd / "target" / "pulumi-java-codegen-testdata"
    val pulumiRepo = sparseCheckout(
      pulumiRepoPath,
      "github.com/pulumi/pulumi.git",
      List(
        os.rel / "pkg" / "codegen" / "testing" / "test" / "testdata"
      )
    )
    val pulumiJavaRepo = sparseCheckout(
      pulumiJavaRepoPath,
      "github.com/pulumi/pulumi-java.git",
      List(
        os.rel / "pkg" / "codegen" / "testing" / "test" / "testdata"
      )
    )
    val targetPath = cwd / "integration-tests" / "resources" / "testdata"
    os.remove.all(targetPath)

    // copy test schemas
    copySchemas(pulumiRepo / "pkg" / "codegen" / "testing" / "test" / "testdata", targetPath)
    copySchemas(pulumiJavaRepo / "pkg" / "codegen" / "testing" / "test" / "testdata", targetPath)

    println("fetched test schema files")

  def copySchemas(sourcePath: os.Path, targetPath: os.Path): Unit =
    println(s"copying from $sourcePath to $targetPath")

    val allowDirList = List(
      // from Pulumi repo
      "secrets",
      "simple-plain-schema",
      "simple-plain-schema-with-root-package",
      "simple-enum-schema",
      "simple-resource-schema",
      "simple-resource-with-aliases",
      "simple-methods-schema",
      "simple-methods-schema-single-value-returns",
      "simple-yaml-schema",
      "simplified-invokes",
      "nested-module",
      "nested-module-thirdparty",
      "enum-reference",
      "external-resource-schema",
      "external-enum",
      "different-enum",
      "embedded-crd-types",
      "dash-named-schema",
      "hyphen-url",
      "hyphenated-symbols",
      "naming-collisions",
      "provider-config-schema",
      "replace-on-change",
      "resource-property-overlap",
      "cyclic-types",
      "plain-and-default",
      "plain-object-defaults",
      "plain-object-disable-defaults",
      "different-package-name-conflict",
      "azure-native-nested-types",
      "functions-secrets",
      "assets-and-archives",
      "dashed-import-schema",
      "other-owned",
      "output-funcs-edgeorder",
      "output-funcs",
      "provider-type-schema",
      "provider-config-schema",
      "urn-id-properties",
      "unions-inside-arrays",
      "methods-return-plain-resource",
      // from Pulumi Java repo
      "mini-azurenative",
      "mini-awsnative",
      "mini-awsclassic",
      "mini-azuread",
      "mini-awsx",
      "mini-kubernetes",
      "jumbo-resources"
    )

    val allowFileList = List()

    val allowExtensions = List("json", "yaml")

    copyFilteredFiles(sourcePath, targetPath, allowDirList, allowFileList, allowExtensions)
  end copySchemas
end Schemas
