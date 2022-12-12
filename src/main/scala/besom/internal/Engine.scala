package besom.internal

import besom.util.NonEmptyString
import pulumirpc.engine.EngineGrpc.EngineStub
import pulumirpc.engine.*

trait Engine[F[+_]]:
  def getRootResource(getRootResource: GetRootResourceRequest): F[GetRootResourceResponse]
  def setRootResource(setRootResource: SetRootResourceRequest): F[SetRootResourceResponse]
  def log(logRequest: LogRequest): F[Unit]
  def close(): F[Unit]

class EngineImpl[F[+_]](private val stub: EngineStub, private val closeF: () => F[Unit])(using F: Monad[F])
    extends Engine[F]:
  def getRootResource(getRootResource: GetRootResourceRequest): F[GetRootResourceResponse] =
    F.fromFuture(stub.getRootResource(getRootResource))

  def setRootResource(setRootResource: SetRootResourceRequest): F[SetRootResourceResponse] =
    F.fromFuture(stub.setRootResource(setRootResource))

  def log(logRequest: LogRequest): F[Unit] =
    F.fromFuture(stub.log(logRequest)).void

  def close(): F[Unit] = closeF()

object Engine:
  def apply[F[+_]](monitorAddr: NonEmptyString)(using F: Monad[F]): F[Engine[F]] = F.evalTry {
    netty.channel.build(monitorAddr).map { channel =>
      new EngineImpl(EngineStub(channel), () => F.eval(channel.shutdown()).void)
    }
  }
