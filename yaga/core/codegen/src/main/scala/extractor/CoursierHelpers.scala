package yaga.codegen.core.extractor

object CoursierHelpers:
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
