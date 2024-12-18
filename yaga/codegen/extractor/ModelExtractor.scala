package yaga.codegen.core.extractor

import tastyquery.Contexts.*
import tastyquery.Types.*
import tastyquery.Traversers.TreeTraverser
import tastyquery.Symbols.*
import tastyquery.Definitions
import tastyquery.Names

class ModelExtractor():
  import ModelExtractor.*

  protected val referencedSymbols = collection.mutable.Set.empty[ClassSymbol]
  protected val typesToVisit = collection.mutable.Queue.empty[Type]

  def enqueueType(tpe: Type) =
    typesToVisit.enqueue(tpe)

  def collect(rootTypes: Seq[Type])(using Context) =
    referencedSymbols.clear()
    typesToVisit.clear()
    rootTypes.foreach(enqueueType)
    while typesToVisit.nonEmpty do
      val tpe = typesToVisit.dequeue()
      traverseType(tpe)
    referencedSymbols.toSet

  private def notSupported(msg: String): Nothing =
    throw Exception(s"Not supported by yaga codegen: ${msg}")

  private def notSupported(tpe: Type): Nothing =
    notSupported(s"type ${tpe.showBasic}")


  def traverseType(tpe: Type)(using Context) =
    tpe match
      case t: TypeRef =>
        visitTypeRef(t)
      case t: TermRef =>
        notSupported(t)
      case t: AppliedType =>
        enqueueType(t.tycon)
        t.args.foreach:
          case arg: Type =>
            enqueueType(arg)
          case _: WildcardTypeArg =>
            {}
      case t: ByNameType =>
        enqueueType(t.underlying)
      case t: ThisType =>
        notSupported(t)
      case t: FlexibleType =>
        notSupported(t)
      case t: OrType =>
        enqueueType(t.first)
        enqueueType(t.second)
      case t: AndType =>
        enqueueType(t.first)
        enqueueType(t.second)
      case t: TypeLambda =>
        notSupported(t)
      case t: TypeParamRef =>
        notSupported(t)
      case t: TermParamRef =>
        notSupported(t)
      case t: AnnotatedType =>
        enqueueType(t.underlying)
      case t: ConstantType =>
        notSupported(t)
      case t: MatchType =>
        notSupported(t)
      case t: TypeRefinement =>
        notSupported(t)
      case t: TermRefinement =>
        notSupported(t)
      case t: RecType =>
        notSupported(t)
      case t: RecThis =>
        notSupported(t)
      case t: SuperType =>
        notSupported(t)
      case t: RepeatedType =>
        notSupported(t)
      case t: SkolemType =>
        notSupported(t)
      case t: NothingType =>
        notSupported(t)
      case t: AnyKindType =>
        notSupported(t)
      case t: CustomTransientGroundType =>
        notSupported(t)


  def visitTypeRef(ref: TypeRef)(using Context): Unit =
    if isBuiltinClass(ref) then
      return

    val cls = ref.optSymbol match
      case None => return // TODO
      case Some(sym) => sym.asClass

    val packageParts = extractPackagePrefixParts(ref).getOrElse(throw Exception(s"Unsupported non-top-level class ${cls.name}"))

    if !cls.isCaseClass then
      throw Exception(s"Unsupported non-case class type ${ref.showBasic}")

    referencedSymbols.add(cls)

    val constructors = cls.declarations.collect:
      case sym: TermSymbol if sym.name == Names.nme.Constructor => sym.asTerm
    val constructor = constructors match
      case Seq(ctor) => ctor
      case _ => throw Exception(s"Expected exactly 1 constructor for a case class ${cls.name} but found ${constructors.size}")

    constructor.declaredType match
      case meth: MethodType =>
        meth.resultType match
          case tpe: Type => // Case class with just a single term parameters list
            meth.paramTypes.foreach(enqueueType)
          case _ =>
            throw Exception(s"Unsupported constructor type ${meth}")

      case poly: PolyType =>
        poly.resultType match
          case meth: MethodType =>
            meth.resultType match
              case tpe: Type => // Generic case class with just a single term parameters list
                meth.paramTypes.foreach(enqueueType)
              case _ =>
                throw Exception(s"Unsupported constructor type ${poly}")
          case _ =>
            throw Exception(s"Unsupported constructor type ${poly}")
      case t =>
        throw Exception(s"Unsupported constructor type ${t}")

    cls.parents.foreach(enqueueType)

  def isBuiltinClass(ref: TypeRef) =
    val fullRefName = ref.showBasic
    fullRefName.startsWith("scala.") || fullRefName.startsWith("java.")

object ModelExtractor:
  def ownerPackageNamesChain(sym: Symbol | Null): Seq[String] =
    sym match
      case pkg: PackageSymbol =>
        if pkg.isRootPackage then
          Seq.empty
        else
          ownerPackageNamesChain(sym.owner) :+ pkg.name.name
      case _ =>
        throw Exception(s"Unsupported non-package symbol ${sym}")
    

  def extractPackagePrefixParts(ref: NamedType): Option[Seq[String]] =
    ref.prefix match
      case NoPrefix => Some(Seq.empty)
      case pkg: PackageRef =>
        Some(ownerPackageNamesChain(pkg.symbol))
      case _ =>
        None
