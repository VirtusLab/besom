package besom.codegen

trait BuildFileGenerator {
  def generateBuildFiles(
    schemaName: SchemaName,
    packageVersion: PackageVersion,
    dependencies: List[(SchemaName, SchemaVersion)],
    pluginDownloadUrl: Option[String],
    packageInfo: PulumiPackageInfo
  )(using config: Config): Seq[SourceFile]

  protected def generatePluginMetadata(
    schemaName: SchemaName,
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
    schemaName: SchemaName,
    packageVersion: PackageVersion,
    dependencies: List[(SchemaName, SchemaVersion)],
    pluginDownloadUrl: Option[String],
    packageInfo: PulumiPackageInfo
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
    schemaName: SchemaName,
    packageVersion: PackageVersion,
    dependencies: List[(SchemaName, SchemaVersion)],
    pluginDownloadUrl: Option[String],
    packageInfo: PulumiPackageInfo
  )(using config: Config): Seq[SourceFile] = {
    val buildFileContent = s"""|name := "besom-${schemaName}"
                         |version := "${packageVersion}-core.${config.coreShortVersion}"
                         |organization := "${config.organization}"
                         |scalaVersion := "${config.scalaVersion}"
                         |
                         |scalacOptions ++= Seq("-java-output-version", "11")
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

object MultiModuleSbtBuildFileGenerator extends BuildFileGenerator {
  def generateBuildFiles(
    schemaName: SchemaName,
    packageVersion: PackageVersion,
    dependencies: List[(SchemaName, SchemaVersion)],
    pluginDownloadUrl: Option[String],
    packageInfo: PulumiPackageInfo
  )(using config: Config): Seq[SourceFile] = {
    // Get top-level packages from the package info
    val topLevelPackages = packageInfo.topLevelPackages
    val subprojects      = topLevelPackages.map(p => s"`$p`").mkString(", ")

    // Root build.sbt with all module configurations
    val rootBuildFile =
      s"""|import sbt.ScopeFilter
          |
          |Global / concurrentRestrictions += Tags.limit(Tags.Compile, 1)
          |
          |lazy val root = (project in file("."))
          |  .dependsOn(${subprojects})
          |  .settings(
          |    name := "besom-${schemaName}",
          |    version := "${packageVersion}-core.${config.coreShortVersion}",
          |    organization := "${config.organization}",
          |    scalaVersion := "${config.scalaVersion}",
          |    scalacOptions ++= Seq("-java-output-version", "11"),
          |    libraryDependencies ++= Seq(
          |      "org.virtuslab" %% "besom-core" % "${config.besomVersion}",${dependencies
           .map { case (name, version) =>
             s""""org.virtuslab" %% "besom-$name" % "${version}-core.${config.coreShortVersion}" """
           }
           .mkString("\n", ",\n      ", "")}
          |    ),
          |    Compile / packageBin / mappings ++=
          |      (Compile / packageBin / mappings)
          |        .all(ScopeFilter(inProjects(${subprojects})))
          |        .value
          |        .flatten
          |  )
          |
          |${topLevelPackages
           .map { pkg =>
             s"""|lazy val `$pkg` = (project in file("$pkg"))
                |  .settings(
                |    scalaVersion := "${config.scalaVersion}",
                |    scalacOptions ++= Seq("-java-output-version", "11"),
                |    libraryDependencies ++= Seq(
                |      "org.virtuslab" %% "besom-core" % "${config.besomVersion}",${dependencies
                  .map { case (name, version) =>
                    s""""org.virtuslab" %% "besom-$name" % "${version}-core.${config.coreShortVersion}" """
                  }
                  .mkString("\n", ",\n      ", "")}
                |    ),
                |    publish / skip := true
                |  )
                |""".stripMargin
           }
           .mkString("\n")}
          |""".stripMargin

    // Plugin metadata
    val pluginMetadata = SourceFile(
      FilePath(Seq("src", "main", "resources", "besom", "api", schemaName, "plugin.json")),
      generatePluginMetadata(schemaName, packageVersion, pluginDownloadUrl)
    )

    // Plugins file
    val pluginsFile = SourceFile(
      FilePath(Seq("project", "plugins.sbt")),
      s"""|addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.0")
          |addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
          |""".stripMargin
    )

    Seq(
      SourceFile(FilePath(Seq("build.sbt")), rootBuildFile),
      pluginMetadata,
      pluginsFile
    )
  }
}
