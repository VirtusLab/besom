import besom.*

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  val name     = config.requireString("name")
  val hush     = config.requireString("hush")
  val notThere = config.get[String]("notThere").getOrElse("default value")

  val codeSecret = Output.secret("secret code")
  val viral1     = p"""${codeSecret} is the secret code"""
  val viral2     = name.flatMap(n => hush.map(h => n + h))
  val viral3     = hush.flatMap(h => name.map(n => n + h))

  import spray.json.DefaultJsonProtocol._
  val names = config.requireObject[List[String]]("names")

  import spray.json.JsonFormat
  case class Foo(name: String, age: Int)
  //noinspection ScalaUnusedSymbol
  object Foo:
    given JsonFormat[Foo] = jsonFormat2(Foo.apply)

  val foo = config.requireObject[Foo]("foo")

  Output(
    exports(
      name = name,
      hush = hush,
      notThere = notThere,
      codeSecret = codeSecret,
      viral1 = viral1,
      viral2 = viral2,
      viral3 = viral3,
      names = names,
      foo = foo.map(summon[JsonFormat[Foo]].write(_))
    )
  )
}
