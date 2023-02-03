package com.pulumi.bootstrap.internal

import scala.util.Using
import io.github.classgraph.ClassGraph
import io.github.classgraph.Resource
import spray.json._

object PulumiPlugins:
  import scala.jdk.CollectionConverters._

  final private val PluginRegex = "^(com/pulumi/(.+))/plugin.json$".r

  def fromClasspath: Map[String, JsValue] =
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
