package yaga.sbt.aws

import _root_.sbt._
import java.nio.file.Path
import yaga.sbt.MavenArtifactsHelpers

private[aws] object CodegenHelpers {
  def runCodegen(localJarSources: Seq[Path], packagePrefix: String, outputDir: Path, withInfra: Boolean, lambdaArtifactAbsolutePath: Option[Path], lambdaRuntime: Option[String], log: Logger): Unit = {
    val mainArgs = codegenMainArgs(localJarSources, packagePrefix, outputDir, withInfra, lambdaArtifactAbsolutePath, lambdaRuntime)

    log.info(s"Running yaga AWS codegen with args: ${mainArgs.mkString(" ")}")

    MavenArtifactsHelpers.runMavenArtifactMainWithArgs(
      "org.virtuslab", "yaga-aws-lambda-codegen_3", YagaAwsLambdaPlugin.yagaAwsVersion,
      "yaga.codegen.aws.runCodegen",
      mainArgs
    )
  }

  private def codegenMainArgs(localJarSources: Seq[Path], packagePrefix: String, outputDir: Path, withInfra: Boolean, lambdaArtifactAbsolutePath: Option[Path], lambdaRuntime: Option[String]): Seq[String] = {
    val infraFlag =
      if (withInfra)
        Seq("--with-infra")
      else
        Seq.empty

    val infraMainArgs = (
      infraFlag ++
      lambdaArtifactAbsolutePath.map(path => Seq("--lambda-artifact-absolute-path", path.toString)).getOrElse(Nil) ++
      lambdaRuntime.map(runtime => Seq("--lambda-runtime", runtime)).getOrElse(Nil)
    )

    val sourcesOptions = localJarSources.flatMap(path => Seq("--local-classpath-jar", path.toString))

    val mainArgs = sourcesOptions ++ Seq(
      "--package-prefix", packagePrefix,
      "--output-dir", outputDir.toString,
    ) ++ infraMainArgs
    mainArgs
  }
}