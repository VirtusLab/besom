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
  inline def lambdaHandlerMetadataFromMavenCoordinates[Config, Input, Output](
    inline orgName: String,
    inline moduleName: String,
    inline version: String
  ): LambdaHandlerMetadata = ${ lambdaHandlerMetadataFromMavenCoordinatesImpl[Config, Input, Output]('orgName, 'moduleName, 'version) }

  private def lambdaHandlerMetadataFromMavenCoordinatesImpl[C : Type, I : Type, O : Type](
    orgNameExpr: Expr[String],
    moduleNameExpr: Expr[String],
    versionExpr: Expr[String]
  )(using Quotes): Expr[LambdaHandlerMetadata] =
    val orgName = orgNameExpr.valueOrAbort
    val moduleName = moduleNameExpr.valueOrAbort
    val version = versionExpr.valueOrAbort

    val allHandlersMetadata = extractLambdaHandlersMetadataFromMavenCoordinates(orgName = orgName, moduleName = moduleName, version = version)
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


  private def extractLambdaHandlersMetadataFromMavenCoordinates(
    orgName: String, moduleName: String, version: String
  ): Seq[LambdaHandlerMetadata] =
    val jatPath = getJarPathFromMavenCoordinates(orgName = orgName, moduleName = moduleName, version = version)
    val metadata = extractLambdaHandlersMetadataFromLocalJar(jatPath)
    metadata

  private def extractLambdaHandlersMetadataFromLocalJar(
    absoluteJarPath: Path
  ): Seq[LambdaHandlerMetadata] =
    import io.github.classgraph.ClassGraph
    import scala.jdk.CollectionConverters.*

    val jarUrlString = s"file://${absoluteJarPath}"
    val jarClassLoader = new java.net.URLClassLoader(Array(java.net.URL(jarUrlString)))

    val lambdaHandlerClassFullName = classOf[LambdaHandler[?, ?, ?]].getName

     // TODO filter out classes from transitive dependencies?
    val lambdaHandlerSubclasses = new ClassGraph().overrideClassLoaders(jarClassLoader).enableClassInfo.scan().getSubclasses(lambdaHandlerClassFullName).iterator.asScala.toList

    lambdaHandlerSubclasses.map: classInfo =>
      val handlerClass = classInfo.loadClass()
      val handlerClassName = handlerClass.getName

      val shapeInstanceMethod =
        val lambdaShapeClassName = classOf[LambdaShape[?]].getName
        val methodsByType = handlerClass.getMethods.filter(_.getReturnType.getName == lambdaShapeClassName).toList
        val instanceMethod = methodsByType match
          case meth :: Nil =>
            meth
          case _ =>
            throw new Exception(s"Expected exactly one instance of ${lambdaShapeClassName} for ${handlerClassName} but found ${methodsByType.length}")
        assert(Modifier.isStatic(instanceMethod.getModifiers), s"Method ${instanceMethod.getName} in class ${handlerClass.getName} must but static")
        instanceMethod

      val shapeInstance = shapeInstanceMethod.invoke(null)

      val shapeClass = shapeInstance.getClass
      val configSchema = shapeClass.getMethod("configSchema").invoke(shapeInstance).asInstanceOf[String]
      val inputSchema = shapeClass.getMethod("inputSchema").invoke(shapeInstance).asInstanceOf[String]
      val outputSchema = shapeClass.getMethod("outputSchema").invoke(shapeInstance).asInstanceOf[String]

      LambdaHandlerMetadata(
        handlerClassName = classInfo.getName,
        artifactAbsolutePath = absoluteJarPath.toString,
        configSchema = configSchema,
        inputSchema = inputSchema,
        outputSchema = outputSchema
      )

  private def getJarPathFromMavenCoordinates(orgName: String, moduleName: String , version: String): java.nio.file.Path =
    import coursier._

    // TODO Verify performance and try to speed up compilation
    val fetchedFiles = Fetch()
      .withRepositories(Seq(
        // TODO allow customization of repositories
        LocalRepositories.ivy2Local,
        Repositories.central
      ))
      .addDependencies(
        Dependency(Module(Organization(orgName), ModuleName(moduleName)), version)
      )
      .run()

    val expectedJarFileName= s"${moduleName}.jar"

    fetchedFiles.filter(_.getName == expectedJarFileName) match
      case Nil =>
        throw new Exception(s"No file with name $expectedJarFileName found when getting paths of resolved dependencies. All paths:\n ${fetchedFiles.mkString("\n")}.")
      case file :: Nil => file.toPath
      case files =>
        throw new Exception(s"Expected exactly one file with name $expectedJarFileName when getting paths of resolved dependencies. Instead found: ${files.mkString(", ")}.")
