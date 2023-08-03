import besom.*
import besom.api.random.{randomPet}
@main def main = Pulumi.run {
  for 
    randomPet <- randomPet("randomPetServer")
    name <- randomPet.id
  yield Pulumi.exports(
    name = name
  )
}