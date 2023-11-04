package besom.integration.codegen

import besom.integration.common.*
import munit.{Slow, TestOptions}
import os.*

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

  val testdata = os.pwd / "integration-tests" / "resources" / "testdata"

  val slowList = List(
    "external-enum" // depends on google-native
  )

  // FIXME: broken - codegen error
  val ignoreList = List(
    "secrets",
    "simple-plain-schema",
    "simple-plain-schema-with-root-package",
    "simple-resource-schema",
    "simple-enum-schema",
    "simple-methods-schema",
    "simple-methods-schema-single-value-returns",
    "simple-yaml-schema",
    "external-enum", // depends on google-native, and the dep does not compile
    "external-resource-schema",
    "enum-reference",
    "different-enum",
    "embedded-crd-types",
    "hyphen-url",
    "naming-collisions",
    "mini-azurenative",
    "jumbo-resources",
    "replace-on-change",
    "resource-property-overlap",
    "cyclic-types",
    "plain-and-default",
    "different-package-name-conflict",
    "azure-native-nested-types"
  )

  val tests =
    for schema <- os
        .walk(testdata)
        .filter(os.isFile(_))
        .filter(f => List("json", "yaml").contains(f.ext))
    yield
      // noinspection ScalaUnusedSymbol
      val name = schema match {
        case _ / d / g"schema.$ext" => d
        case _ / g"$name.$ext"      => name
      }
      TestData(name, schema)

  case class TestData(
    name: String,
    schema: os.Path
  )

  tests.foreach { data =>
    val name = s"""schema '${data.name}' (${data.schema.relativeTo(testdata)}) should codegen"""
    // noinspection ScalaUnusedSymbol
    val options: TestOptions = data.name match {
      case n if ignoreList.contains(n) => name.ignore
      case n if slowList.contains(n)   => name.tag(Slow)
      case _                           => name
    }
    test(options) {
      val result = codegen.generatePackageFromSchema(data.schema)
      if (result.dependencies.nonEmpty)
        println(s"\nCompiling dependencies for ${result.schemaName}...")
      for (dep <- result.dependencies) {
        codegen.generateLocalPackage(dep)
      }
      println(s"\nCompiling generated code for ${data.name}...")
      val compiled = scalaCli.compile(result.outputDir).call(check = false)
      assert {
        clue(data)
        compiled.exitCode == 0
      }
      println()
    }
  }
}
