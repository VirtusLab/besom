package besom.internal

import besom.util.NonEmptyString
import pulumirpc.engine.EngineGrpc.EngineStub
import pulumirpc.engine.*

trait Engine:
  def getRootResource(getRootResource: GetRootResourceRequest): Result[GetRootResourceResponse]
  def setRootResource(setRootResource: SetRootResourceRequest): Result[SetRootResourceResponse]
  def log(logRequest: LogRequest): Result[Unit]
  def close(): Result[Unit]

class EngineImpl(private val stub: EngineStub, private val closeFn: () => Result[Unit]) extends Engine:
  def getRootResource(getRootResource: GetRootResourceRequest): Result[GetRootResourceResponse] =
    Result.deferFuture(stub.getRootResource(getRootResource))

  def setRootResource(setRootResource: SetRootResourceRequest): Result[SetRootResourceResponse] =
    Result.deferFuture(stub.setRootResource(setRootResource))

  def log(logRequest: LogRequest): Result[Unit] =
    Result.deferFuture(stub.log(logRequest)).void

  def close(): Result[Unit] = closeFn()

object Engine:
  def apply(monitorAddr: NonEmptyString): Result[Engine] = Result.evalTry {
    netty.channel.build(monitorAddr).map { channel =>
      new EngineImpl(EngineStub(channel), netty.channel.awaitTermination(channel))
    }
  }
