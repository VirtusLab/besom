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
    def call(callRequest: CallRequest): Result[CallResponse] =
      Result.fail(Exception("Not implemented"))
    def invoke(invokeRequest: ResourceInvokeRequest): Result[InvokeResponse] =
      Result.fail(Exception("Not implemented"))
    def readResource(readResourceRequest: ReadResourceRequest): Result[ReadResourceResponse] =
      Result.fail(Exception("Not implemented"))
    def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse] =
      Result.fail(Exception(s"Not implemented\n${pprint.apply(registerResourceRequest)}"))
    def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): Result[Unit] =
      Result.fail(Exception("Not implemented"))
    def supportsFeature(supportsFeatureRequest: SupportsFeatureRequest): Result[SupportsFeatureResponse] =
      Result.fail(Exception("Not implemented"))
    def close(): Result[Unit] = Result.fail(Exception("Not implemented"))

  val dummyEngine = new Engine:
    def getRootResource(getRootResource: GetRootResourceRequest): Result[GetRootResourceResponse] =
      Result.fail(Exception("Not implemented"))
    def setRootResource(setRootResource: SetRootResourceRequest): Result[SetRootResourceResponse] =
      Result.fail(Exception("Not implemented"))
    def log(logRequest: LogRequest): Result[Unit] =
      Result.fail(Exception("Not implemented"))
    def close(): Result[Unit] =
      Result.fail(Exception("Not implemented"))

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
      memo         <- Memo()
      config       <- Config(runInfo.project, isProjectName = true, configMap = configMap, configSecretKeys = configSecretKeys)
      resources    <- Resources()
      given Context = Context.create(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, resources, memo, stackPromise)
      _ <- stackPromise.fulfill(StackResource()(using ComponentBase(Output(besom.types.URN.empty))))
    yield summon[Context]

end DummyContext
