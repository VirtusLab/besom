package besom.internal

import pulumirpc.resource.*
import pulumirpc.provider.CallRequest
import pulumirpc.provider.CallResponse
import pulumirpc.provider.InvokeResponse
import pulumirpc.engine.*
import besom.NonEmptyString
import besom.internal.logging.BesomLogger

object DummyContext:
  val dummyRunInfo        = RunInfo(Some("test-organization"), "test-project", "test-stack", true, 4, false, "dummy", "dummy")
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
    featureSupport: FeatureSupport = dummyFeatureSupport,
    monitor: Monitor = dummyMonitor,
    engine: Engine = dummyEngine,
    configMap: Map[NonEmptyString, String] = Map.empty,
    configSecretKeys: Set[NonEmptyString] = Set.empty
  ): Result[Context] =
    for
      taskTracker  <- TaskTracker()
      stackPromise <- Promise[StackResource]()
      logger       <- BesomLogger.local()
      config       <- Config(runInfo.project, isProjectName = true, configMap = configMap, configSecretKeys = configSecretKeys)
      resources    <- Resources()
    yield Context(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, resources, stackPromise)
