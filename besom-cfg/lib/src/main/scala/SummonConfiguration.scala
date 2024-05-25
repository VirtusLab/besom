package besom.cfg

import io.github.classgraph.ClassGraph
import scala.util.Using
import scala.jdk.CollectionConverters.*
import besom.json.JsonWriter
import besom.cfg.internal.*

object SummonConfiguration:

  private val toSkip = Set(
    "org.scala-lang",
    "io.github.classgraph",
    "org.virtuslab.besom-cfg", // self
    "org.virtuslab.besom-json"
  )

  def main(args: Array[String]): Unit =
    def classPath = new ClassGraph()
      .filterClasspathElements(path => toSkip.forall(segment => !path.contains(segment)))
      .enableClassInfo()
      .scan()

    Using(classPath) { scanResult =>
      val classes = scanResult
        .getClassesImplementing(classOf[Configured[_]])
        .loadClasses()
        .asScala
        .toSet

      if classes.size > 1 then
        throw Exception(
          "Multiple Configured instances found! Only one per application is allowed."
        )

      if classes.isEmpty then
        throw Exception(
          "No Configured instances found! Exactly one is required."
        )

      val clazz = classes.head

      val maybeNoArgConst = clazz
        .getDeclaredConstructors()
        .filter(_.getParameterCount() == 0)
        .headOption

      val maybeSingleArgConst = clazz
        .getDeclaredConstructors()
        .filter(_.getParameterCount() == 1)
        .headOption

      val instance = maybeNoArgConst
        .map(_.newInstance().asInstanceOf[Configured[_]])
        .getOrElse {
          // this works with the assumption that user used `derives` to create the instance
          // and therefore the class is placed in the companion object
          maybeSingleArgConst
            .map { ctor =>
              val moduleClass = ctor.getParameterTypes().head
              val moduleField =
                moduleClass.getDeclaredFields().find(_.getName == "MODULE$").get

              val outer = moduleField.get(null)

              ctor.newInstance(outer).asInstanceOf[Configured[_]]
            }
            .getOrElse {
              throw Exception(
                "No compatible constructor found for Configured instance!"
              )
            }
        }

      println(summon[JsonWriter[Schema]].write(instance.schema))
    }.get
  end main
end SummonConfiguration
