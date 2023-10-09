package besom.internal

import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.epoll.{Epoll, EpollDomainSocketChannel, EpollEventLoopGroup}
import io.netty.channel.kqueue.{KQueue, KQueueDomainSocketChannel, KQueueEventLoopGroup}
import io.netty.channel.unix.DomainSocketAddress

import java.io.IOException
import java.util.concurrent.TimeUnit
import scala.util.Try

object netty {
  object channel {

    def builderFor(url: String): Try[NettyChannelBuilder] =
      Try {
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
      }

    // MaxRpcMessageSizeInBytes raises the gRPC Max Message size from `4194304` (4mb) to `419430400` (400mb)
    private val MaxRpcMessageSizeInBytes: Int = 400 * 1024 * 1024

    def build(url: String): Try[ManagedChannel] =
      builderFor(url).map { builder =>
        builder
          .usePlaintext() // disable TLS
          .maxInboundMessageSize(MaxRpcMessageSizeInBytes)
          .build()
      }

    def awaitTermination(channel: ManagedChannel): () => Result[Unit] = { () =>
      Result.defer(channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)).void
    }
  }
}
