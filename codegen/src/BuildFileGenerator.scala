package besom.codegen

trait BuildFileGenerator {
  def generateBuildFiles(
    schemaName: String,
    packageVersion: PackageVersion,
    dependencies: List[(SchemaName, SchemaVersion)],
    pluginDownloadUrl: Option[String]
  )(using config: Config): Seq[SourceFile]

  protected def generatePluginMetadata(
    schemaName: String,
    packageVersion: PackageVersion,
    pluginDownloadUrl: Option[String]
  ): String = {
    val pluginDownloadUrlJsonValue = pluginDownloadUrl match {
      case Some(url) => s"\"${url}\""
      case None      => "null"
    }
    s"""|{
        |  "resource": true,
        |  "name": "${schemaName}",
        |  "version": "${packageVersion}",
        |  "server": ${pluginDownloadUrlJsonValue}
        |}
        |""".stripMargin
  }
}

object ScalaCliBuildFileGenerator extends BuildFileGenerator {
  def generateBuildFiles(
    schemaName: String,
    packageVersion: PackageVersion,
    dependencies: List[(SchemaName, SchemaVersion)],
    pluginDownloadUrl: Option[String]
  )(using config: Config): Seq[SourceFile] = {
    val besomVersion      = config.besomVersion
    val scalaVersion      = config.scalaVersion
    val javaVersion       = config.javaVersion
    val javaTargetVersion = config.javaTargetVersion
    val coreShortVersion  = config.coreShortVersion
    val organization      = config.organization
    val url               = config.url
    val vcs               = config.vcs
    val license           = config.license
    val repository        = config.repository
    val developers        = config.developers

    val developersBlock = developers.map(developer => s"//> using publish.developer \"$developer\"").mkString("\n")

    val dependenciesBlock = dependencies
      .map { case (name, version) =>
        s"""|//> using dep "org.virtuslab::besom-${name}:${version}-core.${config.coreShortVersion}"
            |""".stripMargin
      }
      .mkString("\n")

    val buildFileContent =
      s"""|//> using scala "$scalaVersion"
          |//> using jvm "$javaVersion"
          |//> using options "-java-output-version:$javaTargetVersion"
          |//> using options "-skip-by-regex:.*"
          |
          |//> using dep "org.virtuslab::besom-core:${besomVersion}"
          |${dependenciesBlock}
          |//> using resourceDir "resources"
          |
          |//> using publish.name "besom-${schemaName}"
          |//> using publish.organization "$organization"
          |//> using publish.version "${packageVersion}-core.${coreShortVersion}"
          |//> using publish.url "$url"
          |//> using publish.vcs "$vcs"
          |//> using publish.license "$license"
          |//> using publish.repository "$repository"
          |${developersBlock}
          |""".stripMargin

    Seq(
      SourceFile(FilePath(Seq("project.scala")), buildFileContent),
      SourceFile(
        FilePath(Seq("resources", "besom", "api", schemaName, "plugin.json")),
        generatePluginMetadata(schemaName, packageVersion, pluginDownloadUrl)
      )
    )
  }
}

object SbtBuildFileGenerator extends BuildFileGenerator {
  def generateBuildFiles(
    schemaName: String,
    packageVersion: PackageVersion,
    dependencies: List[(SchemaName, SchemaVersion)],
    pluginDownloadUrl: Option[String]
  )(using config: Config): Seq[SourceFile] = {
    val buildFileContent = s"""|name := "besom-${schemaName}"
                         |version := "${packageVersion}-core.${config.coreShortVersion}"
                         |organization := "${config.organization}"
                         |scalaVersion := "${config.scalaVersion}"
                         |
                         |scalacOptions ++= Seq("-release", "11")
                         |
                         |libraryDependencies ++= Seq(
                         |  "org.virtuslab" %% "besom-core" % "${config.besomVersion}",
                         |${dependencies
                                .map { case (name, version) =>
                                  s"""  "org.virtuslab" %% "besom-${name}" % "${version}-core.${config.coreShortVersion}" """
                                }
                                .mkString(",\n")}
                         |)
                         |""".stripMargin

    val pluginsFileContent =
      s"""|addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.0")
          |addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
          |""".stripMargin

    Seq(
      SourceFile(FilePath(Seq("build.sbt")), buildFileContent),
      SourceFile(FilePath(Seq("project", "plugins.sbt")), pluginsFileContent),
      SourceFile(
        FilePath(Seq("src", "main", "resources", "besom", "api", schemaName, "plugin.json")),
        generatePluginMetadata(schemaName, packageVersion, pluginDownloadUrl)
      )
    )
  }
}
