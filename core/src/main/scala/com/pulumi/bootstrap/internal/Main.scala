//> using resourceDir "../../../../../resources" // nice.

package com.pulumi.bootstrap.internal
import spray.json._

object Main:
  def main(args: Array[String]): Unit =
    val foundPlugins = PulumiPlugins.fromClasspath
    print(JsArray(foundPlugins.values.toVector).compactPrint)
