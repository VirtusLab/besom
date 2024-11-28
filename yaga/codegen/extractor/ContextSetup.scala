package yaga.codegen.core

import tastyquery.Contexts.*
import tastyquery.Types.*
import tastyquery.Traversers.TreeTraverser
import tastyquery.Symbols.*
import tastyquery.Definitions
import tastyquery.Names

object ContextSetup:
  def contextFromJar(jarPath: String): Context =
    contextFromJar(java.nio.file.Paths.get(jarPath))

  def contextFromJar(jarPath: java.nio.file.Path): Context =
    val basePath = java.nio.file.FileSystems.getFileSystem(java.net.URI.create("jrt:/")).getPath("modules", "java.base")
    val cp = List(
      basePath,
      jarPath
    )
    val classpath = tastyquery.jdk.ClasspathLoaders.read(cp)
    Context.initialize(classpath)

  def contextFromMavenCoordinates(orgName: String, moduleName: String , version: String): Context =
    val jarPath = getJarPathFromMavenCoordinates(orgName, moduleName, version)
    contextFromJar(jarPath)

  // TODO copy-pasted from LambdaHandlerUtils
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


  //////////////////
  // For debug purposes:
  //////////////////

  def getUrlClassLoader(classloader: ClassLoader): java.net.URLClassLoader =
    classloader match
      case classloader: java.net.URLClassLoader => classloader
      case _ => getUrlClassLoader(classloader.getParent)

  def getReplClasspathUrls(): Seq[java.nio.file.Path] =
    val urlClassLoader = getUrlClassLoader(getClass.getClassLoader)
    urlClassLoader.getURLs().toSeq.collect:
      case url if url.toString.contains("besom/yaga") => java.nio.file.Paths.get(url.toURI)

  def testReplContext(): Context =
    val currentPaths = getReplClasspathUrls()
    val cp = getReplClasspathUrls().toList
    val classpath = tastyquery.jdk.ClasspathLoaders.read(cp)
    Context.initialize(classpath)
