package besom.plugin

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
import dotty.tools.dotc.core.Types.{ TypeRef, TypeBounds, Type }
import dotty.tools.dotc.transform.{ Pickler, Staging }
import dotty.tools.dotc.core.Types.WildcardType

class BesomPlugin extends StandardPlugin {
  val name: String = "BesomCompilerPlugin"
  override val description: String = "Additional support and linting for Besom"

  def init(options: List[String]): List[PluginPhase] =
    (new BesomPhase) :: Nil
}

class BesomPhase extends PluginPhase {
  import tpd.*

  val phaseName = "BesomPhase"

  override val runsAfter = Set(Pickler.name)
  override val runsBefore = Set(Staging.name)

  private var OutputClass: Symbol = _
  private var OutputType: Type = _

  override def prepareForUnit(tree: Tree)(using ctx: Context): Context = {
    OutputClass = requiredClass("besom.internal.Output")
    OutputType = OutputClass.typeRef.appliedTo(TypeBounds.empty)
    traverser.traverse(tree)
    ctx
  }
  
  private def extractRepeated(tree: Tree): List[Tree] = tree match {
    case Typed(tree, _) => extractRepeated(tree)
    case SeqLiteral(elems, _) => elems.flatMap(extractRepeated)
    case tree => List(tree)
  }

  private def traverser = new TreeTraverser {
    override def traverse(tree: tpd.Tree)(using ctx: Context): Unit = {
      val newCtx = if tree.symbol.exists then ctx.withOwner(tree.symbol) else ctx
      tree match {
        case tree@Apply(fun, args) =>
          val isDefaultInterpolation = fun.symbol == defn.StringContext_s || fun.symbol == defn.StringContext_f || fun.symbol == defn.StringContext_raw
          val output = args.flatMap(extractRepeated).find(_.typeOpt <:< OutputType)
          output match
            case Some(output) if isDefaultInterpolation =>
              report.error(em"""|An expression of the 'Output' type, specifically:
                                |$output
                                |is used in a default string interpolator.
                                |
                                |This will most likely lead to unintended behavior.
                                |Use the pulumi interpolator instead.
                                |e.g.
                                |pulumi"your text"""".mapMsg(_.stripMargin), output.srcPos)
            case _ =>
        case _ => 
      }
      traverseChildren(tree)(using newCtx)
    }
  }
}
