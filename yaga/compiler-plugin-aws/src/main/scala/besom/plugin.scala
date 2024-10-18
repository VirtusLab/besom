package yaga.plugin.aws

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.*
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.StdNames.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.report
import dotty.tools.dotc.reporting.NoExplanation
import dotty.tools.dotc.core.Types.{TypeRef, TypeBounds, Type}
import dotty.tools.dotc.transform.{Pickler, Staging}
import dotty.tools.dotc.core.Types.WildcardType

class YagaAwsLambdaPlugin extends StandardPlugin {
  val name: String                 = "YagaAwsLambdaPlugin"
  override val description: String = "Adding annotations for subclasses of ShapedRequestHandler"

  def init(options: List[String]): List[PluginPhase] =
    List(new YagaAwsLambdaPhase)
}

class YagaAwsLambdaPhase extends PluginPhase {
  import tpd.*

  val phaseName = "YagaAwsLambdaPhase"

  override val runsAfter  = Set(Pickler.name)
  override val runsBefore = Set(Staging.name)

  // private var OutputClass: Symbol = _
  // private var OutputType: Type    = _

  // override def prepareForUnit(tree: Tree)(using ctx: Context): Context = {
  //   // OutputClass = requiredClass("besom.internal.Output")
  //   // OutputType = OutputClass.typeRef.appliedTo(TypeBounds.empty)
  //   traverser.traverse(tree)
  //   ctx
  // }

  override def transformTypeDef(typeDef: TypeDef)(using Context): TypeDef = {
    // Add your annotation here
    // val newAnnotation = tpd.EmptyTree // Select(Ident(TypeRef(NoPrefix, "MyAnnotation")), "apply")

    // val annotationClassSymbol = requiredClass("yaga.extensions.aws.lambda.annotations.ConfigSchema")

    val annotationArgument = Literal(StringConstant("Your message here"))
    // val newAnnotation = Apply(
    //   Select(Ident(TypeRef(NoPrefix, "foo.MyAnnot")), "apply"),
    //   List(annotationArgument)
    // )

    val newAnnotation = Apply(Select(New(Select(Select(Select(Select(Select(Ident("yaga"),"extensions"),"aws"),"lambda"),annotations),MyAnnot)),<init>),List(Literal(Constant(qwe))))



    // // val updatedClassDef = typeDef.withMods(classDef.mods.withAnnotations(newAnnotation :: classDef.mods.annotations))
    // // updatedClassDef
    // typeDef.withMods(typeDef.mods.withAddedAnnotation(newAnnotation))

    println(typeDef.getClass.getName)
    println(typeDef.show)
    println(requiredClass("yaga.extensions.aws.lambda.annotations.MyAnnot"))
    println(requiredClass("yaga.extensions.aws.lambda.annotations.MyAnnot").typeRef)
    println(typeDef.mods.annotations)
    println("*************")


    typeDef
  }

  // override def transformAnnotated(annotated: Annotated)(using Context): Annotated =
  //   println(annotated)
  //   println("*************")
  //   annotated
}
