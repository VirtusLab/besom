package besom.bootstrap

import scala.util.Using
import scala.jdk.CollectionConverters.*
import java.io.{File, PrintWriter}
import io.github.classgraph.ClassGraph
import besom.json.*

object PulumiPluginsDiscoverer:
  def main(args: Array[String]): Unit =
    args.toList match
      case Nil =>
        print(pluginsJsonText)
      case "--output-file" :: outputFilePath :: Nil =>
        writeToFile(path = outputFilePath, text = pluginsJsonText)
      case _ =>
        Console.err.println(s"PulumiPluginDiscoverer: Wrong main arguments: ${args.mkString(",")}")
        sys.exit(1)

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

  private def pluginsJsonText =
    val foundPlugins = pluginsFromClasspath
    JsArray(foundPlugins.values.toVector).compactPrint

  private def writeToFile(path: String, text: String): Unit =
    val file = new File(path)
    val writer = new PrintWriter(file)

    writer.write(text)
    writer.close()
