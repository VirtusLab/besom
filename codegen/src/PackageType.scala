package besom.codegen

sealed trait PackageType {
  def name: String
  def buildFileGenerator: BuildFileGenerator
  def isSbt: Boolean
}

case object ScalaCliPackage extends PackageType {
  def name: String                           = "scala-cli"
  def buildFileGenerator: BuildFileGenerator = ScalaCliBuildFileGenerator
  def isSbt: Boolean                         = false
}

case object SbtPackage extends PackageType {
  def name: String                           = "sbt"
  def buildFileGenerator: BuildFileGenerator = SbtBuildFileGenerator
  def isSbt: Boolean                         = true
}

case object MultiModuleSbtPackage extends PackageType {
  def name: String                           = "multi-module-sbt"
  def buildFileGenerator: BuildFileGenerator = MultiModuleSbtBuildFileGenerator
  def isSbt: Boolean                         = true
}
