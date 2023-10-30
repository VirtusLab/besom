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
  override def munitFlakyOK = true

  val testdata = os.pwd / "integration-tests" / "resources" / "testdata"

  val slowFileList = List(
    "kubernetes",
    "docker",
    "digitalocean"
  )
  val slowDirList = List(
    "akamai"
  )

  // FIXME: less broken - compilation error
  val flakyFileList = List(
    "digitalocean"
  )
  val flakyDirList = List(
    "simple-enum-schema",
    "external-enum"
  )

  // FIXME: broken - codegen error
  val ignoreFileList = List(
    "digitalocean"
  )
  val ignoreDirList = List(
    "secrets",
    "simple-plain-schema",
    "simple-plain-schema-with-root-package",
    "simple-resource-schema",
    "simple-methods-schema",
    "simple-methods-schema-single-value-returns",
    "simple-yaml-schema",
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
    val options: TestOptions = data.schema match {
      case _ / g"$f.$ext" if ignoreFileList.contains(f) => name.ignore
      case _ / d / _ if ignoreDirList.contains(d)       => name.ignore
      case _ / g"$f.$ext" if slowFileList.contains(f)   => name.tag(Slow)
      case _ / d / _ if slowDirList.contains(d)         => name.tag(Slow)
      case _ / g"$f.$ext" if flakyFileList.contains(f)  => name.flaky
      case _ / d / _ if flakyDirList.contains(d)        => name.flaky
      case _                                            => name
    }
    test(options) {
      val outputDir = codegen.generatePackageFromSchema(data.schema, data.name, "0.0.0")
      val compiled  = scalaCli.compile(outputDir).call(check = false)
      assert {
        clue(data)
        compiled.exitCode == 0
      }
      println()
    }
  }
}
