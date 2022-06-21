//> using scala "3.1.2"

//> using lib "com.thesamet.scalapb::scalapb-runtime:0.11.10"
//> using lib "io.grpc:grpc-netty:1.45.0"
//> using lib "io.netty:netty-transport-native-kqueue:4.1.77.Final"
//> using lib "io.netty:netty-transport-native-epoll:4.1.77.Final"
//> using lib "com.thesamet.scalapb::scalapb-runtime-grpc:0.11.10"

package besom

import com.google.protobuf.struct.Value.Kind.NumberValue
import com.google.protobuf.struct.{Struct, Value}
import pulumirpc.resource.{
  RegisterResourceOutputsRequest,
  RegisterResourceRequest,
  RegisterResourceResponse
}
import pulumirpc.engine.SetRootResourceRequest
import pulumirpc.engine.EngineGrpc.EngineStub
import pulumirpc.engine.EngineGrpc

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Failure, Success}

object netty {
  object channel {

    import io.grpc.ManagedChannel
    import io.grpc.netty.NettyChannelBuilder
    import io.netty.channel.epoll.{Epoll, EpollDomainSocketChannel, EpollEventLoopGroup}
    import io.netty.channel.kqueue.{KQueue, KQueueDomainSocketChannel, KQueueEventLoopGroup}
    import io.netty.channel.unix.DomainSocketAddress

    import java.io.IOException

    @throws[IOException]
    def builder(url: String): NettyChannelBuilder =
      url match {
        case url if url.startsWith("unix:") =>
          val address = new DomainSocketAddress(url.replaceFirst("^unix:", ""))
          val builder = NettyChannelBuilder.forAddress(address)
          if (Epoll.isAvailable)
            builder
              .channelType(classOf[EpollDomainSocketChannel])
              .eventLoopGroup(new EpollEventLoopGroup)
          else if (KQueue.isAvailable)
            builder
              .channelType(classOf[KQueueDomainSocketChannel])
              .eventLoopGroup(new KQueueEventLoopGroup)
          else
            throw new IOException(
              "Unix domain sockets are unsupported on this platform"
            )
        case url => NettyChannelBuilder.forTarget(url)
      }

    // MaxRpcMessageSizeInBytes raises the gRPC Max Message size from `4194304` (4mb) to `419430400` (400mb)
    val MaxRpcMessageSizeInBytes: Int = 400 * 1024 * 1024

    @throws[IOException]
    def build(url: String): ManagedChannel = builder(url)
      .usePlaintext() // disable TLS
      .maxInboundMessageSize(MaxRpcMessageSizeInBytes)
      .build()
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    import io.grpc.ManagedChannel
    import pulumirpc.resource.ResourceMonitorGrpc

    import scala.concurrent.ExecutionContext.Implicits.global

    val project       = sys.env("PULUMI_PROJECT")
    val stack         = sys.env("PULUMI_STACK")
    val monitorTarget = sys.env("PULUMI_MONITOR")
    println("PULUMI_MONITOR=" + monitorTarget)

    val pulumiEngine = sys.env("PULUMI_ENGINE")
    println("PULUMI_ENGINE=" + pulumiEngine)

    def urn(project: String, stack: String, `type`: String, name: String) =
      s"urn:pulumi:$stack::$project::${`type`}::$name"

    val rootStackType = s"pulumi:pulumi:Stack"
    val rootStackName = s"$project-$stack"

    val engineChannel: ManagedChannel = netty.channel.build(pulumiEngine)
    val engineStub                    = EngineGrpc.blockingStub(engineChannel)

    val setRootResourceRequest = SetRootResourceRequest(
      urn(project, stack, rootStackType, rootStackName)
    )

    println("SetRootResourceRequest=" + setRootResourceRequest)
    val setRootResourceResponse = engineStub.setRootResource(setRootResourceRequest)
    println("SetRootResourceResponse=" + setRootResourceResponse)

    val monitorChannel: ManagedChannel = netty.channel.build(monitorTarget)
    val monitorStub                    = ResourceMonitorGrpc.blockingStub(monitorChannel)

    val randomIntRegistrationRequest = RegisterResourceRequest(
      `type` = "random:index/randomInteger:RandomInteger",
      name = "my-int",
      //   version = randomVersion,
      custom = true,
      `object` = Some(
        Struct.of(
          Map(
            "min" -> Value.of(NumberValue(0)),
            "max" -> Value.of(NumberValue(10))
          )
        )
      )
    )

    println("RegisterResourceRequest=" + randomIntRegistrationRequest)
    val r: RegisterResourceResponse = monitorStub.registerResource(randomIntRegistrationRequest)
    println("RegisterResourceResponse=" + r)

    val r2 = monitorStub.registerResourceOutputs(
      RegisterResourceOutputsRequest(
//        urn = urn(project, stack, rootStackType, rootStackName),
        urn = r.urn,
        outputs = r.`object`
      )
    )
    println("RegisterResourceOutputsRequest=" + r2)
  }
}
