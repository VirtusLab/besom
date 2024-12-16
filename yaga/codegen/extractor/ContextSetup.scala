package yaga.codegen.core.extractor

import tastyquery.Contexts.*
import tastyquery.Types.*
import tastyquery.Traversers.TreeTraverser
import tastyquery.Symbols.*
import tastyquery.Definitions
import tastyquery.Classpaths.*

object ContextSetup:
  def jarPathsFromMavenCoordinates(orgName: String, moduleName: String , version: String): List[java.nio.file.Path] =
    import coursier._

    // TODO Verify performance and try to speed up compilation
    Fetch()
      .withRepositories(Seq(
        // TODO allow customization of repositories
        LocalRepositories.ivy2Local,
        Repositories.central
      ))
      .addDependencies(
        Dependency(Module(Organization(orgName), ModuleName(moduleName)), version)
      )
      .run()
      .map(_.toPath)
      .toList

  def contextFromCodegenSources(codegenSources: List[CodegenSource]): Context =
    val basePath = java.nio.file.FileSystems.getFileSystem(java.net.URI.create("jrt:/")).getPath("modules", "java.base")
    val cp = basePath +: getSourcesClasspath(codegenSources)
    val classpath = tastyquery.jdk.ClasspathLoaders.read(cp)
    Context.initialize(classpath)

  def getSourcesClasspath(codegenSources: Seq[CodegenSource]): List[java.nio.file.Path] =
    codegenSources.flatMap {
      case CodegenSource.MavenArtifact(org, module, version) =>
        jarPathsFromMavenCoordinates(org, module, version)
      case CodegenSource.LocalJar(path) =>
        List(path)
    }.toList

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

  def testReplClasspath(): Classpath =
    val currentPaths = getReplClasspathUrls()
    val cp = getReplClasspathUrls().toList
    tastyquery.jdk.ClasspathLoaders.read(cp)

  def testReplContext(): Context =
    val classpath = testReplClasspath()
    Context.initialize(classpath)
