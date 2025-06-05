import besom.*
import besom.api.random.*
import besom.internal.ResourceArgsTransformation
import besom.internal.TransformedResourceInfo

case class RandomZoo(
  lionNameLength: Output[Int],
  rhinoNameLength: Output[Int]
)(using ComponentBase)
    extends ComponentResource
    derives RegistersOutputs

object RandomZoo:
  extension (c: Output[RandomZoo])
    def lionNameLength: Output[Int]  = c.flatMap(_.lionNameLength)
    def rhinoNameLength: Output[Int] = c.flatMap(_.rhinoNameLength)

  def apply(using Context)(name: NonEmptyString, options: ComponentResourceOptions = ComponentResourceOptions()): Output[RandomZoo] =
    component(name, "custom:resource:RandomZoo", options) {
      val randomPet1 = RandomPet(
        "lion",
        RandomPetArgs(length = 10)
      )

      val randomPet2 = RandomPet(
        "rhino",
        RandomPetArgs(length = 20)
      )

      RandomZoo(lionNameLength = randomPet1.length, rhinoNameLength = randomPet2.length)
    }

@main def main(): Unit = Pulumi.run {
  val lionTransformation = RandomPet.whenNamed("lion").transformArgs { case args: RandomPetArgs =>
    args.withArgs(length = 15)
  }

  val zoo = RandomZoo(
    "zoo",
    ComponentResourceOptions(
      transformations = List(lionTransformation)
    )
  )

  Stack.exports(
    lionNameLength = zoo.lionNameLength,
    rhinoNameLength = zoo.rhinoNameLength
  )
}
