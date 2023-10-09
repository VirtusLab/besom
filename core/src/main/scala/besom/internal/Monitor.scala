package besom.internal

import besom.util.NonEmptyString
import pulumirpc.resource.ResourceMonitorGrpc.ResourceMonitorStub
import pulumirpc.resource.*
import pulumirpc.provider.CallRequest
import pulumirpc.provider.CallResponse
import pulumirpc.provider.InvokeResponse

trait Monitor:
  def call(callRequest: CallRequest): Result[CallResponse]
  def invoke(invokeRequest: ResourceInvokeRequest): Result[InvokeResponse]
  def readResource(readResourceRequest: ReadResourceRequest): Result[ReadResourceResponse]
  def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse]
  def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): Result[Unit]
  def supportsFeature(supportsFeatureRequest: SupportsFeatureRequest): Result[SupportsFeatureResponse]
  def close(): Result[Unit]

class MonitorImpl(private val stub: ResourceMonitorStub, private val closeFn: () => Result[Unit]) extends Monitor:

  def call(callRequest: CallRequest): Result[CallResponse] = Result.deferFuture(stub.call(callRequest))

  def invoke(invokeRequest: ResourceInvokeRequest): Result[InvokeResponse] =
    Result.deferFuture(stub.invoke(invokeRequest))

  def readResource(readResourceRequest: ReadResourceRequest): Result[ReadResourceResponse] =
    Result.deferFuture(stub.readResource(readResourceRequest))

  def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse] =
    Result.deferFuture(stub.registerResource(registerResourceRequest))

  def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): Result[Unit] =
    Result.deferFuture(stub.registerResourceOutputs(registerResourceOutputsRequest)).void

  def supportsFeature(supportsFeatureRequest: SupportsFeatureRequest): Result[SupportsFeatureResponse] =
    Result.deferFuture(stub.supportsFeature(supportsFeatureRequest))

  def close(): Result[Unit] = closeFn()

object Monitor:
  def apply(monitorAddr: NonEmptyString): Result[Monitor] = Result.evalTry {
    netty.channel.build(monitorAddr).map { channel =>
      new MonitorImpl(ResourceMonitorStub(channel), netty.channel.awaitTermination(channel))
    }
  }
