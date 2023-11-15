import besom.*

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  for
    name <- config.requireString("name")
    hush <- config.requireString("hush")
    notThere <- config.get[String]("notThere").getOrElse("default value")
  yield exports(
    name = name,
    hush = hush,
    notThere = notThere
  )
}