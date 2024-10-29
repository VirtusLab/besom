package yaga.extensions.aws.lambda

import scala.util.Try
import scala.reflect.ClassTag
import scala.quoted.*
import java.nio.file.{Files, Paths}
import java.lang.reflect.Modifier

import yaga.shapes.Schema

import besom.types.Archive

object ShapedLambdaUtils:
  inline def lambdaHandlerMetadataFromLocalJar[Config, Input, Output](
    inline jarPath: String,
    inline handlerClassName: String
  ): LambdaHandlerMetadata[Config, Input, Output] = ${ lambdaHandlerMetadataFromLocalJarImpl[Config, Input, Output]('jarPath, 'handlerClassName) }

  private def lambdaHandlerMetadataFromLocalJarImpl[C : Type, I : Type, O : Type](
    jarPathExpr: Expr[String],
    handlerClassNameExpr: Expr[String]
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

    val handlerClass = classLoader.loadClass(handlerClassName)
    val handlerSuperClassName = handlerClass.getSuperclass.getName
    val expectedSuperClassName = classOf[LambdaHandler[?, ?, ?]].getName
    assert(handlerSuperClassName == expectedSuperClassName, s"The handler class $handlerClassName does not inherit directly from $expectedSuperClassName")

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

    val expectedConfigSchema = Schema.getSchemaStr[C]
    val expectedInputSchema = Schema.getSchemaStr[I]
    val expectedOutputSchema = Schema.getSchemaStr[O]

    assert(
      areSchemasCompatible(expectedSchema = expectedConfigSchema, actualSchema = configSchema),
      s"Lambda handler config schema mismatch; Expected: $expectedConfigSchema; Declared in classfile: $configSchema"
    )

    assert(
      areSchemasCompatible(expectedSchema = expectedInputSchema, actualSchema = inputSchema),
      s"Lambda handler input schema mismatch; Expected: $expectedInputSchema; Declared in classfile: $inputSchema"
    )

    assert(
      areSchemasCompatible(expectedSchema = expectedOutputSchema, actualSchema = outputSchema),
      s"Lambda handler output schema mismatch; Expected: $expectedOutputSchema; Declared in classfile: $outputSchema"
    )

    val jarAbsolutePathExpr = Expr(jarAbsolutePath.toString)

    '{
      LambdaHandlerMetadata[C, I, O](
        codeArchive = Archive.FileArchive(${ jarAbsolutePathExpr }),
        handlerClassName = ${ handlerClassNameExpr }
      )
    }

  private def areSchemasCompatible(expectedSchema: String, actualSchema: String) =
    // TODO 1: Parse schemas and compare structurally
    // TODO 2: Compare semantically, e.g. optional fields, default values?
    expectedSchema == actualSchema
