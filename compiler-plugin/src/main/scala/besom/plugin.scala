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
import scala.collection.mutable.ListBuffer

class BesomPlugin extends StandardPlugin {
  val name: String = "BesomCompilerPlugin"
  override val description: String = "Additional support and linting for Besom"

  def init(options: List[String]): List[PluginPhase] =
    (new BesomPhase) :: Nil
}

/*
TODO:
  - [x] check if valdef is unused
  - [x] check if bind is unused
  - [x] check if a statement is of type Output
  - [x] check if Output is cast to a non-output type (typer should insert {...; ()}, so this should be the same as the previous point)
  - [x] check if Output is unused in a for comprehension
  - [x] check that it doesn't report an unused error when an output value is ignored explicitly
  - [x] check what happens if Output is unused in a method call argument e.g. flatMap
  - [ ] error when Output is used in foreach (needs special case), since the signture of foreach is ~`[U] => List[A] => (A => U) => Unit`  
  - [ ] handle ignoredOutput annotation
  - [ ] not error unused class members
*/

/**
  * This phase is responsible for linting and additional support for Besom.
  * It does:
  * - when an Output value is used in a default string interpolator, it reports an error
  * - when an Output value is unused, it reports an error
  * 
  * The unused part is heavily inspired by the CheckUnused phase in the dotty compiler
  */
class BesomPhase extends PluginPhase {
  import tpd.*

  val phaseName = "BesomPhase"

  override val runsAfter = Set(Pickler.name)
  override val runsBefore = Set(Staging.name)

  private var OutputClass: Symbol = _
  private var OutputType: Type = _

  private val unusedOutputSymbols = ListBuffer[Symbol]()

  enum ScopeType:
    case Other, Local, Class

  object ScopeType:
    def fromTree(tree: Tree): ScopeType = tree match {
      case _: Template => ScopeType.Class
      case _: Block => ScopeType.Local
      case _ => ScopeType.Other
    }

  private var currentScope: ScopeType = ScopeType.Other

  override def prepareForUnit(tree: Tree)(using ctx: Context): Context = {
    OutputClass = requiredClass("besom.internal.Output")
    OutputType = OutputClass.typeRef.appliedTo(TypeBounds.empty)
    traverser.traverse(tree)
    unusedOutputSymbols.foreach { symbol =>
      report.error(em"""|Unused Output value
                        |""".mapMsg(_.stripMargin), symbol.srcPos)
    }
    ctx
  }

  private def extractRepeated(tree: Tree): List[Tree] = tree match {
    case Typed(tree, _) => extractRepeated(tree)
    case SeqLiteral(elems, _) => elems.flatMap(extractRepeated)
    case tree => List(tree)
  }

  extension (sym: Symbol)
    /** Annotated with @unused */
    private def isUnusedAnnot(using ctx: Context): Boolean =
      sym.annotations.exists(a => a.symbol == ctx.definitions.UnusedAnnot)

    /** A function is overriden. Either has `override flags` or parent has a matching member (type and name) */
    private def isOverriden(using Context): Boolean =
      sym.is(Flags.Override) ||
        (sym.exists && sym.owner.thisType.parents.exists(p => sym.matchingMember(p).exists))

    private def shouldNotReportParamOwner(using ctx: Context): Boolean =
      if sym.exists then
        val owner = sym.owner
        owner.isPrimaryConstructor ||
        owner.annotations.exists ( // @depreacated
          _.symbol == ctx.definitions.DeprecatedAnnot
        ) ||
        owner.isAllOf(Flags.Synthetic | Flags.PrivateLocal) ||
        owner.is(Flags.Accessor) ||
        owner.isOverriden
      else
        false

  extension (memDef: tpd.MemberDef)
    private def isValidMemberDef(using Context): Boolean =
      memDef.symbol.exists
        && !memDef.symbol.isUnusedAnnot
        && !memDef.symbol.isAllOf(Flags.AccessorCreationFlags)
        && !memDef.symbol.owner.is(Flags.ExtensionMethod)

    private def isValidParam(using Context): Boolean =
      val sym = memDef.symbol
      (sym.is(Flags.Param) || sym.isAllOf(Flags.PrivateParamAccessor | Flags.Local, butNot = Flags.CaseAccessor))
        && !sym.shouldNotReportParamOwner

    private def shouldReportPrivateDef(using ctx: Context): Boolean =
      currentScope == ScopeType.Class && !memDef.symbol.isConstructor && memDef.symbol.is(Flags.Private, butNot = Flags.SelfName | Flags.Synthetic | Flags.CaseAccessor)

  def registerDef(memDef: tpd.MemberDef)(using ctx: Context): Unit =
    if memDef.isValidMemberDef && (memDef.isValidParam || currentScope == ScopeType.Local || memDef.shouldReportPrivateDef) then
      unusedOutputSymbols += memDef.symbol

  private def traverser = new TreeTraverser {
    override def traverse(tree: tpd.Tree)(using ctx: Context): Unit = {
      val newCtx = if tree.symbol.exists then ctx.withOwner(tree.symbol) else ctx
      tree match {
        case valdef@ValDef(name, tpt, rhs) if tpt.tpe <:< OutputType =>
          registerDef(valdef)
        case bind@Bind(name, body) if body.tpe <:< OutputType =>
          unusedOutputSymbols += bind.symbol
        case casedef@CaseDef(ident@Ident(name), guard, body) if name == nme.WILDCARD && ident.tpe <:< OutputType =>
          report.error(em"""|An expression of the 'Output' type is ignored using a wildcard.
                            |
                            |This will most likely lead to unintended behavior.
                            |If you want to ignore the value, use the 'IgnoredOutput' extractor instead.
                            |""".mapMsg(_.stripMargin), ctx.owner.srcPos)
        case ident@Ident(name) if ident.tpe <:< OutputType =>
          unusedOutputSymbols -= ident.symbol
        case block@Block(stats, expr) =>
          stats
            .filter(!_.isDef)
            .filter(_.typeOpt <:< OutputType)
            .foreach { stat =>
              report.error(em"""|An expression of the 'Output' type is used in a statement position.
                                |
                                |This value will most likely be ignored. This will most likely lead to unintended behavior.
                                |""".mapMsg(_.stripMargin), stat.srcPos)
            }
        case tree@Apply(fun, args) =>
          val isDefaultInterpolation = fun.symbol == defn.StringContext_s || fun.symbol == defn.StringContext_f || fun.symbol == defn.StringContext_raw
          val output = args.flatMap(extractRepeated).find(_.typeOpt <:< OutputType)
          output match
            case Some(output) if isDefaultInterpolation =>
              report.error(em"""|An expression of the 'Output' type, is used in a default string interpolator.
                                |
                                |This will most likely lead to unintended behavior.
                                |Use the pulumi interpolator instead.
                                |e.g.
                                |pulumi"your text"""".mapMsg(_.stripMargin), output.srcPos)
            case _ =>
        case _ =>
      }
      val oldScope = currentScope
      currentScope = ScopeType.fromTree(tree)
      traverseChildren(tree)(using newCtx)
      currentScope = oldScope
    }
  }
}
