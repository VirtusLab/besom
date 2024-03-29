package besom.integration.codegen

import besom.codegen.PackageMetadata
import besom.integration.common.*
import munit.{Slow, TestOptions}
import os.*

import scala.concurrent.duration.*

//noinspection ScalaWeakerAccess,TypeAnnotation,ScalaFileName
class CodegenTests extends munit.FunSuite {
  override val munitTimeout = 30.minutes

  override def munitTests(): Seq[Test] = super
    .munitTests()
    .filterNot(tags(Slow)) // to run slow tests you need to explicitly enable them with `--include-categories=Slow`
    .filterNot(tagsWhen(isCI)(LocalOnly))

  val testdata = os.pwd / "integration-tests" / "resources" / "testdata"

  val slowList = List(
    "enum-reference", // depends on google-native
    "external-enum", // depends on google-native
    "hyphen-url", // depends on azure-native
    "external-resource-schema" // depends on kubernetes, aws, random
  )

  // FIXME: broken - codegen error
  val ignoreList = List(
    "simple-yaml-schema", // YAML is not supported
    "hyphen-url", // depends on azure-native
    "cyclic-types", // YAML schema is not supported
    "different-package-name-conflict" // file duplicate issue, caused by anomaly in schema: multiple packages names
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
      if (result.metadata.dependencies.nonEmpty)
        println(s"\nCompiling dependencies for ${result.metadata.name}...")
      for (dep <- result.metadata.dependencies) {
        codegen.generateLocalPackage(dep)
      }
      println(s"\nCompiling generated code for ${data.name} in ${result.outputDir}...")
      val compiled = scalaCli.compile(result.outputDir).call(check = false)
      assert {
        clue(data)
        clue(compiled.out.text())
        clue(compiled.err.text())
        compiled.exitCode == 0
      }
      println()
    }
  }
}
