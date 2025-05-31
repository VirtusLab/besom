package besom.codegen

sealed trait PackageType {
  def name: String
  def buildFileGenerator: BuildFileGenerator
}

case object ScalaCliPackage extends PackageType {
  def name: String                           = "scala-cli"
  def buildFileGenerator: BuildFileGenerator = ScalaCliBuildFileGenerator
}

case object SbtPackage extends PackageType {
  def name: String                           = "sbt"
  def buildFileGenerator: BuildFileGenerator = SbtBuildFileGenerator
}

// Factory to create package types
object PackageType {
  def fromString(name: String): Option[PackageType] = name.toLowerCase match {
    case "scala-cli" => Some(ScalaCliPackage)
    case "sbt"       => Some(SbtPackage)
    case _           => None
  }
}
