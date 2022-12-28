package besom

import scala.concurrent.*, ExecutionContext.Implicits.global
import besom.util.Protocol

object providers:
  import besom.*

  // def sgOptions(using Context) =

  def exports(): Output[Map[String, Output[Any]]] = ???

@main
def main(): Unit = Pulumi.run {
  import besom.api.*
  import providers.*
  val opts = aws.SecurityGroupOptions(
    name = "web-sg-62a569b",
    ingress = List(
      aws.IngressRule(protocol = Protocol.TCP, fromPort = 80, toPort = 80, cidrBlocks = List("0.0.0.0/0"))
    )
  )

  def instanceOptions(groupName: Output[String]) = aws.InstanceOptions(
    ami = "ami-6869aa05",
    instanceType = "t2.micro",
    securityGroups = List(groupName)
  )

  exports()
}

// def instanceOptions(groupName: Output[String]) = aws.InstanceOptions(
//   ami = "ami-6869aa05",
//   instanceType = "t2.micro",
//   securityGroups = List(groupName)
// )

// @main
// def main(): Unit = Pulumi.run {
//   for
//     group <- aws.ec2.securityGroup("web-sg", sgOptions, CustomResourceOptions(importId = "sg-04aeda9a214730248"))
//     server <- aws.ec2.instance(
//       "web-server",
//       instanceOptions(group.name),
//       CustomResourceOptions(importId = "i-06a1073de86f4adef")
//     )
//   yield exports()
// }

// @main
// def main(): Unit =
//   Pulumi.run {
//     Output(Map.empty)
//   }

// import besom.api.experimental.*
// val podOutput: Output[Pod] = Output(Pod(Output("abc"), Output(List(1, 2, 3))))
// val id = podOutput.id
// val ports: Output[List[Int]] = podOutput.ports

// @main def run() =
//     println("this works")

object monadicsdk:
  trait PulumiIO[A]:
    def map[B](f: A => B): PulumiIO[B]
    def flatMap[B](f: A => PulumiIO[B]): PulumiIO[B]

  trait Resource[A]
  trait Exports
  def exports(stuff: PulumiIO[_]*): Exports = ???
  object Pulumi:
    def unsafeRun(program: PulumiIO[Exports]): Unit = ???

// from codegen
object azure:
  import monadicsdk.*
  case class ResourceGroup(name: PulumiIO[String])
  case class StorageAccount(name: PulumiIO[String])
  def resourceGroup(name: String): PulumiIO[ResourceGroup]                             = ???
  def storageAccount(name: String, rgName: PulumiIO[String]): PulumiIO[StorageAccount] = ???

object experiment:
  import monadicsdk.*
  val monadicPulumi: PulumiIO[Exports] = for {
    rg <- azure.resourceGroup("my-rg")
    sa <- azure.storageAccount("blobs", rg.name)
  } yield exports(rg.name, sa.name)

  Pulumi.unsafeRun(monadicPulumi)

object experiment2:
  import monadicsdk.*
  val monadicPulumi: PulumiIO[Exports] = for {
    rg <- azure.resourceGroup("my-rg")
    sa <- azure.storageAccount("blobs", rg.name)
  } yield exports(rg.name, sa.name)

  Pulumi.unsafeRun(monadicPulumi)

object declarativesdk:
  trait Output[A]
  trait PulumiCtx:
    def register[A](output: Output[A]): Unit

  class Exports(outputs: Output[_]*)

  object Pulumi:
    def run(program: PulumiCtx ?=> Exports): Unit = ???

// from codegen
object decl_azure:
  import declarativesdk.*
  case class ResourceGroup(name: Output[String])
  case class StorageAccount(name: Output[String])
  def ResourceGroup(name: String)(using PulumiCtx): ResourceGroup                           = ???
  def StorageAccount(name: String, rgName: Output[String])(using PulumiCtx): StorageAccount = ???

object decl_experiment:
  import declarativesdk.*
  import decl_azure.*

  Pulumi.run {
    val rg = ResourceGroup("my-rg")
    val sa = StorageAccount("blobs", rg.name)
    Exports()
  }
