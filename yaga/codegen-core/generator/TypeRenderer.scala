package yaga.codegen.core.generator

import tastyquery.Types.*
import tastyquery.Contexts.*
import tastyquery.Symbols.*

import yaga.codegen.core.extractor.ModelExtractor

class TypeRenderer(packagePrefixParts: Seq[String], apiSymbols: Set[Symbol]):
  import TypeRenderer.*

  def typeToCode(tpe: Type)(using Context): meta.Type =
    // TODO handle other types
    tpe match
      case t: TypeRef =>
        val sym = t.optSymbol.getOrElse(throw Exception(s"TypeRef ${t} has no symbol"))
        val basicPrefixParts = prefixNameParts(t.prefix)
        val shiftedPrefixParts =
          if apiSymbols.contains(sym) then
            packagePrefixParts ++ basicPrefixParts
          else
            basicPrefixParts
        meta.Type.Select(
          absolutePackageRef(shiftedPrefixParts),
          meta.Type.Name(t.name.toString)
        )
      case t: TermRef =>
        notSupported(t)
      case t: AppliedType =>
        val argTypes = t.args.map:
          case arg: Type =>
            typeToCode(arg)
          case _: WildcardTypeArg =>
            notSupported("wildcard type parameter")
        meta.Type.Apply(
          typeToCode(t.tycon),
          meta.Type.ArgClause(argTypes)
        )
      case t: ByNameType =>
        notSupported(t)
      case t: ThisType =>
        notSupported(t)
      case t: FlexibleType =>
        notSupported(t)
      case t: OrType =>
        notSupported(t)
      case t: AndType =>
        notSupported(t)
      case t: TypeLambda =>
        notSupported(t)
      case t: TypeParamRef =>
        notSupported(t)
      case t: TermParamRef =>
        notSupported(t)
      case t: AnnotatedType =>
        notSupported(t)
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

  def absolutePackageRef(nameParts: Seq[String]): meta.Term.Ref =
    nameParts.foldLeft[meta.Term.Ref](meta.Term.Name("_root_")):
      case (acc, part) =>
        meta.Term.Select(acc, meta.Term.Name(part))


object TypeRenderer:
  def prefixNameParts(prefix: Prefix)(using Context): Seq[String] =
    prefix match
      case NoPrefix =>
        Seq.empty
      case ref: PackageRef =>
        ref.symbol.fullName.path.map(_.toString)
      case ref: TermRef =>
        prefixNameParts(ref.prefix) :+ ref.symbol.name.toString
      case _ =>
        notSupported(s"Expected package or term reference prefix but got ${prefix}")

  private def notSupported(msg: String): Nothing =
    throw Exception(s"Not supported by yaga codegen: ${msg}")

  private def notSupported(tpe: Type): Nothing =
    notSupported(s"type ${tpe.showBasic}")