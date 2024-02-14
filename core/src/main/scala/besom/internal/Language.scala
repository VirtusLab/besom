package besom.internal

import com.google.protobuf.*
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.grpc.{InsecureServerCredentials, MethodDescriptor, Server, Status}
import pulumirpc.language.*
import pulumirpc.language.LanguageRuntimeGrpc.LanguageRuntime
import pulumirpc.plugin.*

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

trait LanguageRuntimeService extends LanguageRuntime:
  def run(request: RunRequest): Future[RunResponse] =
    request.getInfo
    Future.successful(RunResponse())

  // Unimplemented on purpose
  def getRequiredPlugins(request: GetRequiredPluginsRequest): Future[GetRequiredPluginsResponse] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_GET_REQUIRED_PLUGINS)

  def getPluginInfo(request: empty.Empty): Future[PluginInfo] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_GET_PLUGIN_INFO)

  def installDependencies(request: InstallDependenciesRequest, responseObserver: StreamObserver[InstallDependenciesResponse]): Unit =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_INSTALL_DEPENDENCIES, responseObserver)

  def about(request: empty.Empty): Future[AboutResponse] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_ABOUT)

  def getProgramDependencies(request: GetProgramDependenciesRequest): Future[GetProgramDependenciesResponse] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_GET_PROGRAM_DEPENDENCIES)

  def runPlugin(request: RunPluginRequest, responseObserver: StreamObserver[RunPluginResponse]): Unit =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_RUN_PLUGIN, responseObserver)

  def generateProgram(request: GenerateProgramRequest): Future[GenerateProgramResponse] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_GENERATE_PROGRAM)

  def generateProject(request: GenerateProjectRequest): Future[GenerateProjectResponse] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_GENERATE_PROJECT)

  def generatePackage(request: GeneratePackageRequest): Future[GeneratePackageResponse] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_GENERATE_PACKAGE)

  def pack(request: PackRequest): Future[PackResponse] =
    unimplementedUnaryCall(LanguageRuntimeGrpc.METHOD_PACK)

  private def unimplementedUnaryCall[A, B](methodDescriptor: MethodDescriptor[A, B]): Future[B] =
    Future.failed(
      Status.UNIMPLEMENTED.withDescription(s"Method ${methodDescriptor.getFullMethodName} is unimplemented").asRuntimeException()
    )

  private def unimplementedUnaryCall[A, B](methodDescriptor: MethodDescriptor[A, B], responseObserver: StreamObserver[B]): Unit =
    responseObserver.onError(
      Status.UNIMPLEMENTED.withDescription(s"Method ${methodDescriptor.getFullMethodName} is unimplemented").asRuntimeException()
    )

end LanguageRuntimeService

object LanguageRuntimeService extends LanguageRuntimeService

object LanguageRuntimeServer:
  case class Address(host: String, port: Int):
    override def toString: String = s"$host:$port"

  def apply(service: LanguageRuntimeService)(using ec: ExecutionContext): LanguageRuntimeServer =
    // TODO: detect that nested stack operations are not supported https://github.com/pulumi/pulumi/issues/5058
    val server = NettyServerBuilder
      .forPort(0 /* random port */, InsecureServerCredentials.create())
      .addService(
        LanguageRuntimeGrpc.bindService(service, ec)
      )
      .build
    new LanguageRuntimeServer(server)

end LanguageRuntimeServer

class LanguageRuntimeServer private (server: Server):
  self =>

  import LanguageRuntimeServer.*

  private val ShutdownTimeoutInSeconds = 30

  def start(): Address =
    try server.start()
    catch
      case e: java.io.IOException =>
        throw new RuntimeException("Failed to start LanguageRuntimeServer", e)
    end try

    val _ = sys.addShutdownHook {
      // Use stderr here since the logger may have been reset by its JVM shutdown hook.
      System.err.println("Shutting down LanguageRuntimeServer gRPC server since JVM is shutting down")
      self.stop()
    }

    val address = Address("127.0.0.1", server.getPort)
    println(s"LanguageRuntimeServer started, listening on $address")
    address
  end start

  def stop(): Unit =
    try server.shutdown().awaitTermination(ShutdownTimeoutInSeconds, TimeUnit.SECONDS)
    catch
      case e: InterruptedException =>
        throw new RuntimeException("Error while awaiting for termination of LanguageRuntimeServer", e)
    end try
    println("LanguageRuntimeServer shut down");
  end stop

end LanguageRuntimeServer
