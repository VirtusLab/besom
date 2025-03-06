package yaga.sbt.aws

import _root_.sbt._
import java.nio.file.Path
import yaga.sbt.MavenArtifactsHelpers

private[aws] object CodegenHelpers {
  def runCodegen(localJarSources: Seq[Path], packagePrefix: String, outputDir: Path, withInfra: Boolean, log: Logger): Unit = {
    val mainArgs = codegenMainArgs(localJarSources, packagePrefix, outputDir, withInfra)

    log.info(s"Running yaga AWS codegen with args: ${mainArgs.mkString(" ")}")

    MavenArtifactsHelpers.runMavenArtifactMainWithArgs(
      "org.virtuslab", "yaga-codegen-aws-lambda_3", YagaAwsLambdaPlugin.yagaVersion,
      "yaga.codegen.aws.runCodegen",
      mainArgs
    )
  }

  private def codegenMainArgs(localJarSources: Seq[Path], packagePrefix: String, outputDir: Path, withInfra: Boolean): Seq[String] = {
    val infraMainArgs =
      if (withInfra)
        Seq("--with-infra")
      else
        Seq.empty

    val sourcesOptions = localJarSources.flatMap(path => Seq("--local-jar", path.toString))

    val mainArgs = sourcesOptions ++ Seq(
      "--package-prefix", packagePrefix,
      "--output-dir", outputDir.toString,
    ) ++ infraMainArgs
    mainArgs
  }
}