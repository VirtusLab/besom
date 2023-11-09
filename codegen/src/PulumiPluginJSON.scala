package besom.codegen

import besom.codegen.UpickleApi._

/** Provides additional information about a package's associated Pulumi plugin.
  *
  * For Scala, the content is inside `besom/<provider>/plugin.json` file inside the package.
  *
  * Keep in sync with
  * [[https://github.com/pulumi/pulumi/blob/master/sdk/go/common/resource/plugin/plugin.go#L52 pulumi/sdk/go/common/resource/plugin/plugin.go:52]]
  */
case class PulumiPluginJSON(resource: Boolean, name: Option[String], version: Option[String], server: Option[String])
//noinspection ScalaUnusedSymbol
object PulumiPluginJSON {
  implicit val rw: ReadWriter[PulumiPluginJSON] = macroRW

  def listFrom(json: String): List[PulumiPluginJSON] = read[List[PulumiPluginJSON]](json)
}
