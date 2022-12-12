package besom.internal

import besom.util.NonEmptyString
import pulumirpc.resource.ResourceMonitorGrpc.ResourceMonitorStub
import pulumirpc.resource.*
import pulumirpc.provider.CallRequest
import pulumirpc.provider.CallResponse
import pulumirpc.provider.InvokeResponse

trait Monitor[F[+_]]:
  def call(callRequest: CallRequest): F[CallResponse]
  def invoke(invokeRequest: ResourceInvokeRequest): F[InvokeResponse]
  def readResource(readResourceRequest: ReadResourceRequest): F[ReadResourceResponse]
  def registerResource(registerResourceRequest: RegisterResourceRequest): F[RegisterResourceResponse]
  def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): F[Unit]
  def supportsFeature(supportsFeatureRequest: SupportsFeatureRequest): F[SupportsFeatureResponse]
  def close(): F[Unit]

class MonitorImpl[F[+_]](private val stub: ResourceMonitorStub, private val closeF: () => F[Unit])(using F: Monad[F])
    extends Monitor[F]:

  def call(callRequest: CallRequest): F[CallResponse] = F.fromFuture(stub.call(callRequest))

  def invoke(invokeRequest: ResourceInvokeRequest): F[InvokeResponse] =
    F.fromFuture(stub.invoke(invokeRequest))

  def readResource(readResourceRequest: ReadResourceRequest): F[ReadResourceResponse] =
    F.fromFuture(stub.readResource(readResourceRequest))

  def registerResource(registerResourceRequest: RegisterResourceRequest): F[RegisterResourceResponse] =
    F.fromFuture(stub.registerResource(registerResourceRequest))

  def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): F[Unit] =
    F.fromFuture(stub.registerResourceOutputs(registerResourceOutputsRequest)).void

  def supportsFeature(supportsFeatureRequest: SupportsFeatureRequest): F[SupportsFeatureResponse] =
    F.fromFuture(stub.supportsFeature(supportsFeatureRequest))

  def close(): F[Unit] = closeF()

object Monitor:
  def apply[F[+_]](monitorAddr: NonEmptyString)(using F: Monad[F]): F[Monitor[F]] = F.evalTry {
    netty.channel.build(monitorAddr).map { channel =>
      new MonitorImpl(ResourceMonitorStub(channel), () => F.eval(channel.shutdown()).void)
    }
  }
