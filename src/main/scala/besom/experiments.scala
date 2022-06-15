package besom

// import besom.experimental.*
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
    def resourceGroup(name: String): PulumiIO[ResourceGroup] = ???
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
    def ResourceGroup(name: String)(using PulumiCtx): ResourceGroup = ???
    def StorageAccount(name: String, rgName: Output[String])(using PulumiCtx): StorageAccount = ???


object decl_experiment:
    import declarativesdk.*
    import decl_azure.*

    Pulumi.run {
        val rg = ResourceGroup("my-rg")
        val sa = StorageAccount("blobs", rg.name)
        Exports()
    }
