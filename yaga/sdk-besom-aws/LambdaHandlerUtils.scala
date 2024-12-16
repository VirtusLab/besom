package yaga.extensions.aws.lambda.internal

import scala.util.Try
import scala.reflect.ClassTag
import scala.quoted.*
import java.nio.file.{Files, Path, Paths}
import java.lang.reflect.Modifier
import besom.types.Archive
import yaga.extensions.aws.model.Schema
import yaga.extensions.aws.lambda.{LambdaHandler, LambdaShape}

object LambdaHandlerUtils:
  inline def lambdaHandlerMetadataFromLocalFatJar[Config, Input, Output](
    inline filePath: String,
  ): LambdaHandlerMetadata = ${ lambdaHandlerMetadataFromLocalFatJarImpl[Config, Input, Output]('filePath) }

  private def lambdaHandlerMetadataFromLocalFatJarImpl[C : Type, I : Type, O : Type](
    filePath: Expr[String]
  )(using Quotes): Expr[LambdaHandlerMetadata] =
    val absoluteJarPath = Paths.get(filePath.valueOrAbort) // TODO check if is absolute path and file exists
    val allHandlersMetadata = extractLambdaHandlersMetadataFromLocalJar(absoluteJarPath = absoluteJarPath)
    val matchingHandlerMetadata = findMatchingHandlerMetadata[C, I, O](allHandlersMetadata)

    '{
      LambdaHandlerMetadata(
        handlerClassName = ${ Expr(matchingHandlerMetadata.handlerClassName) },
        artifactAbsolutePath = ${ Expr(matchingHandlerMetadata.artifactAbsolutePath) },
        configSchema = ${ Expr(matchingHandlerMetadata.configSchema) },
        inputSchema = ${ Expr(matchingHandlerMetadata.inputSchema) },
        outputSchema = ${ Expr(matchingHandlerMetadata.outputSchema) }
      )
    }

  private def areSchemasCompatible(expectedSchema: String, actualSchema: String) =
    // TODO 1: Parse schemas and compare structurally
    // TODO 2: Compare semantically, e.g. optional fields, default values?
    expectedSchema == actualSchema

  private def findMatchingHandlerMetadata[C : Type, I : Type, O : Type](metadata: Seq[LambdaHandlerMetadata])(using Quotes): LambdaHandlerMetadata =
    val expectedConfigSchema = Schema.getSchemaStr[C]
    val expectedInputSchema = Schema.getSchemaStr[I]
    val expectedOutputSchema = Schema.getSchemaStr[O]

    val filteredMetadata = metadata.filter(meta => 
      areSchemasCompatible(expectedSchema = expectedConfigSchema, actualSchema = meta.configSchema) &&
      areSchemasCompatible(expectedSchema = expectedInputSchema, actualSchema = meta.inputSchema) &&
      areSchemasCompatible(expectedSchema = expectedOutputSchema, actualSchema = meta.outputSchema)
    )

    filteredMetadata match
      case Nil =>
        val shownRejectedHandlers = metadata.map(meta =>
          s"""|* ${meta.handlerClassName} @ ${meta.artifactAbsolutePath}
              |  config schema: ${meta.configSchema}
              |  input schema:  ${meta.inputSchema}
              |  output schema: ${meta.outputSchema}""".stripMargin
        )
        val errorMsg =
          s"""|No lambda handler with matching shape found.
              |Expected:
              |  config schema: $expectedConfigSchema
              |  input schema:  $expectedInputSchema
              |  output schema: $expectedOutputSchema
              |Rejected handlers:
              |""".stripMargin ++ shownRejectedHandlers.mkString("\n")
        throw new Exception(errorMsg)
      case metadata :: Nil =>
        metadata
      case _ =>
        throw new Exception(s"Multiple matching lambda handlers found") // TODO

  private def extractLambdaHandlersMetadataFromLocalJar(
    absoluteJarPath: Path
  ): Seq[LambdaHandlerMetadata] =
    import scala.jdk.CollectionConverters.*
    import io.github.classgraph.ClassGraph

    val jarUrlString = s"file://${absoluteJarPath}"
    val jarClassLoader = new java.net.URLClassLoader(Array(java.net.URL(jarUrlString)))
    val classGraph = ClassGraph().overrideClassLoaders(jarClassLoader).enableClassInfo.enableMethodInfo().enableAnnotationInfo().scan()
    val lambdaShapeInstances = classGraph.getClassesImplementing("yaga.extensions.aws.lambda.LambdaShape").iterator.asScala.toList

    lambdaShapeInstances.map: lambdaShapeClass =>
      val List(schemaPlaceholderClass) = lambdaShapeClass.getInnerClasses.iterator.asScala.toList
      val schemaAnnotations = schemaPlaceholderClass.getAnnotationInfo.asMap
      val configSchema = schemaAnnotations.get("yaga.extensions.aws.model.annotations.ConfigSchema").getParameterValues.asMap.get("value").getValue.asInstanceOf[String]
      val inputSchema = schemaAnnotations.get("yaga.extensions.aws.model.annotations.InputSchema").getParameterValues.asMap.get("value").getValue.asInstanceOf[String]
      val outputSchema = schemaAnnotations.get("yaga.extensions.aws.model.annotations.OutputSchema").getParameterValues.asMap.get("value").getValue.asInstanceOf[String]
      val handlerClass = lambdaShapeClass.getOuterClasses.iterator.asScala.toList.head // TODO check that this is a handler class fulfiling requirements for RequestStreamHandler subtypes

      LambdaHandlerMetadata(
        handlerClassName = handlerClass.getName,
        artifactAbsolutePath = absoluteJarPath.toString,
        configSchema = configSchema,
        inputSchema = inputSchema,
        outputSchema = outputSchema
      )

  ///////////////////////


  /*
   * TODO
   * The possibility to refer to artifacts from maven was temporarily removed because dependency on coursier messes up the classpath
   * by adding a transtive dependency to org.scala-lang.modules:scala-collection-compat_2.13, which clashes with org.scala-lang.modules:scala-collection-compat_3
   * added by scribe and scalapb that are relied on by besom-core.
   */

  // inline def lambdaHandlerMetadataFromMavenCoordinates[Config, Input, Output](
  //   inline orgName: String,
  //   inline moduleName: String,
  //   inline version: String
  // ): LambdaHandlerMetadata = ${ lambdaHandlerMetadataFromMavenCoordinatesImpl[Config, Input, Output]('orgName, 'moduleName, 'version) }

  // private def lambdaHandlerMetadataFromMavenCoordinatesImpl[C : Type, I : Type, O : Type](
  //   orgNameExpr: Expr[String],
  //   moduleNameExpr: Expr[String],
  //   versionExpr: Expr[String]
  // )(using Quotes): Expr[LambdaHandlerMetadata] =
  //   val orgName = orgNameExpr.valueOrAbort
  //   val moduleName = moduleNameExpr.valueOrAbort
  //   val version = versionExpr.valueOrAbort

  //   val allHandlersMetadata = extractLambdaHandlersMetadataFromMavenCoordinates(orgName = orgName, moduleName = moduleName, version = version)
  //   val matchingHandlerMetadata = findMatchingHandlerMetadata[C, I, O](allHandlersMetadata)

  //   '{
  //     LambdaHandlerMetadata(
  //       handlerClassName = ${ Expr(matchingHandlerMetadata.handlerClassName) },
  //       artifactAbsolutePath = ${ Expr(matchingHandlerMetadata.artifactAbsolutePath) },
  //       configSchema = ${ Expr(matchingHandlerMetadata.configSchema) },
  //       inputSchema = ${ Expr(matchingHandlerMetadata.inputSchema) },
  //       outputSchema = ${ Expr(matchingHandlerMetadata.outputSchema) }
  //     )
  //   }

  // private def extractLambdaHandlersMetadataFromMavenCoordinates(
  //   orgName: String, moduleName: String, version: String
  // ): Seq[LambdaHandlerMetadata] =
  //   val jatPath = getJarPathFromMavenCoordinates(orgName = orgName, moduleName = moduleName, version = version)
  //   val metadata = extractLambdaHandlersMetadataFromLocalJar(jatPath)
  //   metadata

  // private def getJarPathFromMavenCoordinates(orgName: String, moduleName: String , version: String): java.nio.file.Path =
  //   import coursier._

  //   // TODO Verify performance and try to speed up compilation
  //   val fetchedFiles = Fetch()
  //     .withRepositories(Seq(
  //       // TODO allow customization of repositories
  //       LocalRepositories.ivy2Local,
  //       Repositories.central
  //     ))
  //     .addDependencies(
  //       Dependency(Module(Organization(orgName), ModuleName(moduleName)), version)
  //     )
  //     .run()

  //   val expectedJarFileName= s"${moduleName}.jar"

  //   fetchedFiles.filter(_.getName == expectedJarFileName) match
  //     case Nil =>
  //       throw new Exception(s"No file with name $expectedJarFileName found when getting paths of resolved dependencies. All paths:\n ${fetchedFiles.mkString("\n")}.")
  //     case file :: Nil => file.toPath
  //     case files =>
  //       throw new Exception(s"Expected exactly one file with name $expectedJarFileName when getting paths of resolved dependencies. Instead found: ${files.mkString(", ")}.")
