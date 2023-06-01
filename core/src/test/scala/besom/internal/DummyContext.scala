package besom.internal

import pulumirpc.resource.*
import pulumirpc.provider.CallRequest
import pulumirpc.provider.CallResponse
import pulumirpc.provider.InvokeResponse
import pulumirpc.engine.*
import besom.internal.logging.BesomLogger

object DummyContext:
  val dummyRunInfo        = RunInfo("test-project", "test-stack", 4, false, "dummy", "dummy")
  val dummyRunOptions     = RunOptions(true, scribe.Level.Debug, false)
  val dummyFeatureSupport = FeatureSupport(true, true, true, true)
  val dummyMonitor = new Monitor:
    def call(callRequest: CallRequest): Result[CallResponse]                                                  = ???
    def invoke(invokeRequest: ResourceInvokeRequest): Result[InvokeResponse]                                  = ???
    def readResource(readResourceRequest: ReadResourceRequest): Result[ReadResourceResponse]                  = ???
    def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse]  = ???
    def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): Result[Unit] = ???
    def supportsFeature(supportsFeatureRequest: SupportsFeatureRequest): Result[SupportsFeatureResponse]      = ???
    def close(): Result[Unit]                                                                                 = ???

  val dummyEngine = new Engine:
    def getRootResource(getRootResource: GetRootResourceRequest): Result[GetRootResourceResponse] = ???
    def setRootResource(setRootResource: SetRootResourceRequest): Result[SetRootResourceResponse] = ???
    def log(logRequest: LogRequest): Result[Unit]                                                 = ???
    def close(): Result[Unit]                                                                     = ???

  def apply(
    runInfo: RunInfo = dummyRunInfo,
    runOptions: RunOptions = dummyRunOptions,
    featureSupport: FeatureSupport = dummyFeatureSupport,
    monitor: Monitor = dummyMonitor,
    engine: Engine = dummyEngine,
    keepResources: Boolean = false,
    keepOutputValues: Boolean = false
  ): Result[Context] =
    for
      taskTracker <- TaskTracker()
      stack       <- Promise[Stack]()
      logger      <- BesomLogger.local()
      config      <- Config(runInfo.project, Map.empty, Set.empty, logger)
      resources   <- Resources()
    yield Context(
      runInfo,
      runOptions,
      featureSupport,
      config,
      logger,
      monitor,
      engine,
      taskTracker,
      stack,
      resources
    )
