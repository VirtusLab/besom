package lambdamacro

import scala.quoted.*
import java.io.{File, FileWriter}
import java.nio.file.Files
import java.io.OutputStream

case class Lambda(name: String, repr: String)

object Lambda:
  inline def apply[I, O](inline name: String)(inline f: I => O): Lambda = ${
    LambdaImpl.lambda('name, 'f)
  }

object LambdaImpl:
  extension (str: String)
    def removeSubscripts = str.replaceAll("[₀-₉]", "")

  val predefinedScalaStandardPrefixes = Set(
    "scala.",
    "java.",
    "<special-ops>."
  )

  val predefinedLibraryPrefixes = Set(
    "spray."
  )

  val predefinedPrefixes = predefinedScalaStandardPrefixes | predefinedLibraryPrefixes

  def isPackageObject(using Quotes)(sym: quotes.reflect.Symbol) =
    import quotes.reflect.*
    sym.flags.is(Flags.Module) && sym.name.endsWith("$package$")

  def extractDefinitions(using Quotes)(lambdaTree: quotes.reflect.Tree) =
    import quotes.reflect.*

    def isPredefined(sym: Symbol) =
      val fullName = sym.fullName
      predefinedPrefixes.exists(prefix => fullName.startsWith(prefix))

    def topDefinitionSymbols(sym: Symbol): Seq[Symbol] =
      if !sym.exists || sym.flags.is(Flags.Package) then
        Seq.empty
      else
        println("!!!!!!!!")
        println(sym)
        println(sym.fullName)
        println(sym.owner)
        println(isPredefined(sym))
        val owner = sym.owner
        if owner.flags.is(Flags.Package) || isPackageObject(owner) || !owner.exists then
          Seq(sym, sym.companionModule) // Should we explicitly add companion class for an object too?
        else
          topDefinitionSymbols(owner)


    final case class Acc(declsyms: Set[Symbol], refsyms: Set[Symbol]) {
      def freesyms: Set[Symbol] = refsyms -- declsyms
    }

    object Acc {
      def empty: Acc = Acc(Set.empty, Set.empty)
    }

    object ExternalSymbolsAccumulator extends TreeAccumulator[Acc] {
      override def foldTree(acc: Acc, tree: Tree)(owner: Symbol): Acc = 
        tree match
          case tree: Definition =>
            val newAcc = acc.copy(declsyms = acc.declsyms + tree.symbol)
            foldOverTree(newAcc, tree)(tree.symbol)
          case ident: Ident =>
            acc.copy(refsyms = acc.refsyms + ident.symbol)
          case select: Select =>
            val newAcc = acc.copy(refsyms = acc.refsyms + select.symbol)
            foldOverTree(newAcc, select)(owner)
          case ident: TypeIdent =>
            acc.copy(refsyms = acc.refsyms + ident.symbol)
          case select: TypeSelect =>
            val newAcc = acc.copy(refsyms = acc.refsyms + select.symbol)
            foldOverTree(newAcc, select)(owner)
          case bind: Bind =>
            val newAcc = acc.copy(declsyms = acc.declsyms + tree.symbol)
            foldOverTree(newAcc, tree)(tree.symbol)
          case tree =>
            foldOverTree(acc, tree)(owner)
    }


    def externalSymbols(tree: Tree)(using Quotes): List[Symbol] =
      val acc = ExternalSymbolsAccumulator.foldTree(Acc.empty, tree)(Symbol.spliceOwner)
      acc.freesyms.filterNot(isPredefined).toList

    val externalSyms =
      var visited = Set.empty[Symbol]
      var symsToVisit = externalSymbols(lambdaTree).flatMap(topDefinitionSymbols)

      while symsToVisit.nonEmpty do
        val sym = symsToVisit.head
        symsToVisit = symsToVisit.tail
        if sym.exists && !visited.contains(sym) then
          visited = visited + sym
          val newSyms = externalSymbols(sym.tree).flatMap(topDefinitionSymbols)
          symsToVisit = newSyms ++ symsToVisit

      visited

    externalSyms


  def lambda[I: Type, O: Type](name: Expr[String], fExpr: Expr[I => O])(using Quotes): Expr[Lambda] = {
    import quotes.reflect.*    

    val lambdaTree = fExpr.asTerm

    val extractedDefs = extractDefinitions(lambdaTree)
      .filterNot(sym => sym.flags.is(Flags.Module) && sym.isValDef) // eliminate proxies to objects 

    def fullPackageName(sym: Symbol) =
      val owner = sym.owner
      val nonObjectOwner = if isPackageObject(owner) then owner.owner else owner
      nonObjectOwner.fullName

    def packageSymbol(sym: Symbol) =
      val owner = sym.owner
      if isPackageObject(owner) then owner.owner else owner

    def asCode(sym: Symbol) = Printer.TreeCode.show(sym.tree).removeSubscripts

    val auxSourceCodes = extractedDefs.groupBy(packageSymbol).map { (pkgSym, syms) =>
      val packageTopMembers = syms
        .groupBy: sym =>
          if sym.isClassDef then None else Some(sym.owner)
        .flatMap:
          case (None, topLevelSymbols) =>
            topLevelSymbols.map(asCode)
          case (Some(ownerPackageObjectSym), nestedSymbols) =>
            val objectBodyCode = nestedSymbols.map(asCode).mkString("\n\n").linesIterator.map(line => "  " + line).mkString("\n")
            val objectName = ownerPackageObjectSym.name.stripSuffix("$")
            Seq(s"object ${objectName} {\n${objectBodyCode}\n}")

      val pkgName = pkgSym.fullName

      val packageBody = packageTopMembers.mkString("\n\n").linesIterator.map(line => "  " + line).mkString("\n")

      s"package ${pkgName} {\n${packageBody}\n}"
    }.toList

    // TODO wrap lambda in some package?
    val lambdaSourceCode =
      val body = Printer.TreeCode.show(lambdaTree).removeSubscripts.linesIterator.map(line => "  " + line).mkString("\n")
      s"val ${name.valueOrAbort} = \n${body}"


    // TODO: Set matching scala version. Cannot use 3.3.0-RC3 or earlier because of 2 bugs: )
    //
    // TODO: Manage libraries more dynamically
    val directives = """
    |//> using scala "3.3.1-RC1-bin-20230331-7262437-NIGHTLY"
    |//> using dep "io.spray::spray-json:1.3.6"
    """.stripMargin

    val sourceCode = (directives +: lambdaSourceCode +: auxSourceCodes).mkString("\n\n")

    val srcFile = os.temp(suffix = ".scala", deleteOnExit = false)

    os.write.over(srcFile, sourceCode)

    // TODO: Compile with --native
    val compilationResult = os.proc("scala-cli", "compile", srcFile).call(check = false, stderr = os.Pipe)

    if compilationResult.exitCode != 0 then
      report.errorAndAbort(s"Compiling lambda body failed:\n${compilationResult.err.trim()}")
    else
      report.info(sourceCode)

    '{ new Lambda(${ name }, ${ Expr(sourceCode) }) }
  }
