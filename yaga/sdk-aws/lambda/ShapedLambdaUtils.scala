package yaga.extensions.aws.lambda

import scala.util.Try
import scala.reflect.ClassTag
import scala.quoted.*
import java.nio.file.{Files, Paths}

import yaga.shapes.Schema

import besom.types.Archive

object ShapedLambdaUtils:
  inline def lambdaHandlerMetadataFromLocalJar[Config, Input, Output](
    inline jarPath: String,
    inline handlerClassName: String
  ): LambdaHandlerMetadata[Config, Input, Output] = ${ lambdaHandlerMetadataFromLocalJarImpl[Config, Input, Output]('jarPath, 'handlerClassName) }

  private def lambdaHandlerMetadataFromLocalJarImpl[C : Type, I : Type, O : Type](
    jarPathExpr: Expr[String], handlerClassNameExpr: Expr[String]
  )(using Quotes): Expr[LambdaHandlerMetadata[C, I, O]] =
    import quotes.reflect.*
    
    val jarPath = Paths.get(jarPathExpr.valueOrAbort)
    val handlerClassName = handlerClassNameExpr.valueOrAbort

    val expansionFilePath = Position.ofMacroExpansion.sourceFile.getJPath.getOrElse(throw new Exception("Cannot get the source file path in macro expansion"))
    val rootDirectoryPath = expansionFilePath.getParent
    val jarAbsolutePath = rootDirectoryPath.resolve(jarPath).toAbsolutePath.normalize
    
    assert(Files.exists(jarAbsolutePath), s"Jar file at path $jarAbsolutePath doesn't exist")

    val jarUrlString = s"file://${jarAbsolutePath}"
    val classLoader = new java.net.URLClassLoader(Array(java.net.URL(jarUrlString)))

    val clazz = classLoader.loadClass(handlerClassName)
    val className = clazz.getName
    val superClassName = clazz.getSuperclass.getName
    val expectedSuperClassName = classOf[ShapedRequestHandler[?, ?, ?]].getName
    assert(superClassName == expectedSuperClassName, s"The handler class $className does not inherit directly from $expectedSuperClassName")

    val expectedConfigSchema = Schema.getSchemaStr[C]
    val expectedInputSchema = Schema.getSchemaStr[I]
    val expectedOutputSchema = Schema.getSchemaStr[O]

    def getAnnotationValue(annotationClass: Class[?]): String =
      // clazz.getAnnotation[T] doesn't seem to work when getting a class via an external classloader
      val annotationClassName = annotationClass.getName
      val annotation = clazz.getAnnotations.find(_.annotationType.getName == annotationClassName).getOrElse(throw new Exception(s"Annotation $annotationClassName not found on class $handlerClassName"))
      val valueMethod = classLoader.loadClass(annotationClassName).getMethod("value")
      valueMethod.invoke(annotation).asInstanceOf[String]

    import yaga.extensions.aws.lambda.annotations.{ConfigSchema, InputSchema, OutputSchema}

    val providedConfigSchema = getAnnotationValue(classOf[ConfigSchema])
    val providedInputSchema = getAnnotationValue(classOf[InputSchema])
    val providedOutputSchema = getAnnotationValue(classOf[OutputSchema])

    assert(
      areSchemasCompatible(expectedSchema = expectedConfigSchema, providedSchema = providedConfigSchema),
      s"Lambda handler config schema mismatch; Expected: $expectedConfigSchema; Declared in classfile: $providedConfigSchema"
    )

    assert(
      areSchemasCompatible(expectedSchema = expectedInputSchema, providedSchema = providedInputSchema),
      s"Lambda handler input schema mismatch; Expected: $expectedInputSchema; Declared in classfile: $providedInputSchema"
    )

    assert(
      areSchemasCompatible(expectedSchema = expectedOutputSchema, providedSchema = providedOutputSchema),
      s"Lambda handler output schema mismatch; Expected: $expectedOutputSchema; Declared in classfile: $providedOutputSchema"
    )

    val jarAbsolutePathExpr = Expr(jarAbsolutePath.toString)

    '{
      LambdaHandlerMetadata[C, I, O](
        codeArchive = Archive.FileArchive(${ jarAbsolutePathExpr }),
        handlerClassName = ${ handlerClassNameExpr }
      )
    }

  private def areSchemasCompatible(expectedSchema: String, providedSchema: String) =
    // TODO 1: Parse schemas and compare structurally
    // TODO 2: Compare semantically, e.g. optional fields, default values?
    expectedSchema == providedSchema