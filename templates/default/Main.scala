import besom.*
import besom.api.random.*

@main def main = Pulumi.run {
  for 
    randomPet <- RandomPet("randomPetServer")
    name <- randomPet.id
  yield Pulumi.exports(
    name = name
  )
}