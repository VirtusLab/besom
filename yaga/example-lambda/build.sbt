import yaga.sbt.aws.CodegenItem

lazy val child = project.in(file("child-lambda"))
  .settings(
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "yaga-aws" % "0.4.0-SNAPSHOT"
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )

lazy val parent = project.in(file("parent-lambda"))
  .enablePlugins(YagaAwsCodeGenPlugin)
  .settings(
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.12",
      "org.virtuslab" %% "yaga-aws" % "0.4.0-SNAPSHOT",
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    yagaAwsCodegenItems := Seq(
      CodegenItem.ModelFromLocalJars(
        absoluteJarsPaths = (child / Compile / fullClasspathAsJars).value.map(_.data.toPath),
        outputSubdirName = "child-lambda",
        packagePrefix = ""
      )
    ),
  )

lazy val infra = project.in(file("infra"))
  .enablePlugins(YagaAwsCodeGenPlugin)
  .settings(
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "yaga-besom-aws" % "0.4.0-SNAPSHOT"
    ),

    yagaAwsCodegenItems := Seq(
      CodegenItem.InfraFromLocalJars(
        absoluteJarsPaths = Seq(
          (child / assembly).value.toPath
        ),
        outputSubdirName = "child-lambda",
        packagePrefix = "childlambda"
      ),
      CodegenItem.InfraFromLocalJars(
        absoluteJarsPaths = Seq(
          (parent / assembly).value.toPath
        ),
        outputSubdirName = "parent-lambda",
        packagePrefix = "parentlambda"
      )
    )
  )
