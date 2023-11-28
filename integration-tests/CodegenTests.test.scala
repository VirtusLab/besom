package besom.integration.codegen

import besom.codegen.PackageMetadata
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
    "enum-reference", // depends on google-native
    "external-enum", // depends on google-native
    "hyphen-url", // depends on azure-native
    "external-resource-schema" // depends on kubernetes, aws, random
  )

  // FIXME: broken - codegen error
  val ignoreList = List(
    "simple-resource-schema", // resource decoder issue
    "simple-enum-schema", // simple enum is not supported
    "simple-yaml-schema", // YAML is not supported
    "external-enum", // depends on google-native, TODO: check if this is still broken
    "external-resource-schema", // resource decoder issue
    "enum-reference", // depends on google-native, TODO: check if this is still broken
    "different-enum", // simple enum is not supported
    "hyphen-url", // depends on azure-native,
    "naming-collisions", // codec not found
    "mini-azurenative", // simple enum is not supported
    "replace-on-change", // resource decoder issue
    "resource-property-overlap", // resource decoder issue
    "cyclic-types", // YAML schema is not supported
    "plain-and-default", // simple enum is not supported
    "different-package-name-conflict" // duplicate issue
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
      println(s"Test: $name")
      val result = codegen.generatePackageFromSchema(PackageMetadata(data.name), data.schema)
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

  override def beforeAll(): Unit = {
    pproc("scala-cli", "bloop", "exit").call()
  }
}
