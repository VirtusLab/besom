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

  import besom.json.*, DefaultJsonProtocol.*
  val names = config.requireObject[List[String]]("names")

  case class Foo(name: String, age: Int) derives Encoder, JsonFormat

  val foo = config.requireObject[Foo]("foo")

  Stack.exports(
    name = name,
    hush = hush,
    notThere = notThere,
    codeSecret = codeSecret,
    viral1 = viral1,
    viral2 = viral2,
    viral3 = viral3,
    names = names,
    foo = foo
  )
}
