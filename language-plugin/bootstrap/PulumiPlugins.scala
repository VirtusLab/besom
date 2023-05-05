package besom.bootstrap

import scala.util.Using
import scala.jdk.CollectionConverters.*
import io.github.classgraph.ClassGraph
import spray.json.*

object PulumiPluginsDiscoverer:
  def main(args: Array[String]): Unit =
    val foundPlugins = pluginsFromClasspath
    print(JsArray(foundPlugins.values.toVector).compactPrint)

  
  private val PluginRegex = "^(besom/(.+))/plugin.json$".r

  private def pluginsFromClasspath: Map[String, JsValue] =
    Using.resource(startClasspathScan) { scanResult =>
      scanResult.getAllResources.asScala.flatMap { resource =>
        resource.getPath match
          case PluginRegex(pkg, name) =>
            val json = JsonParser(resource.load)
            Some(pkg -> json)
          case _ => None
      }.toMap
    }

  private def startClasspathScan =
    new ClassGraph()
      .filterClasspathElements(_ => true) // todo exclude garbage
      .scan()
