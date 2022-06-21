//> using lib "com.lihaoyi::upickle:1.6.0"
//> using lib "io.github.classgraph:classgraph:4.8.146" // FOR BOOTSTRAP ONLY, DON'T HATE ME

//> using resourceDir "../../../../../resources" // nice.

package com.pulumi.bootstrap.internal

object Main:
  def main(args: Array[String]): Unit =
    val foundPlugins = PulumiPlugins.fromClasspath
    print(ujson.Arr.from(foundPlugins.values))
