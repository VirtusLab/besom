import besom.*
import besom.api.random.*

@main def main = Pulumi.run {
  val randomPet = RandomPet("randomPetServer")
  val name      = randomPet.map(_.id)

  Stack.exports(
    name = name
  )
}
