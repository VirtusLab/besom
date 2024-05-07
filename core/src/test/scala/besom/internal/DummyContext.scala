package besom.internal

import pulumirpc.provider.CallResponse
import pulumirpc.provider.InvokeResponse
import pulumirpc.engine.*
import besom.NonEmptyString
import besom.internal.logging.BesomLogger
import besom.util.printer
import pulumirpc.callback.Callback
import pulumirpc.resource.{
  ReadResourceRequest,
  ReadResourceResponse,
  RegisterResourceOutputsRequest,
  RegisterResourceRequest,
  RegisterResourceResponse,
  ResourceCallRequest,
  ResourceInvokeRequest,
  SupportsFeatureRequest,
  SupportsFeatureResponse
}

//noinspection TypeAnnotation
object DummyContext:
  val dummyRunInfo        = RunInfo(Some("test-organization"), "test-project", "test-stack", true, 4, false, "dummy", "dummy")
  val dummyFeatureSupport = FeatureSupport(true, true, true, true, true)
  val dummyMonitor = new Monitor:
    def call(callRequest: ResourceCallRequest): Result[CallResponse] =
      Result.fail(Exception("Not implemented"))
    def invoke(invokeRequest: ResourceInvokeRequest): Result[InvokeResponse] =
      Result.fail(Exception("Not implemented"))
    def readResource(readResourceRequest: ReadResourceRequest): Result[ReadResourceResponse] =
      Result.fail(Exception("Not implemented"))
    def registerResource(registerResourceRequest: RegisterResourceRequest): Result[RegisterResourceResponse] =
      Result.fail(Exception(s"Not implemented\n${printer.render(registerResourceRequest)}"))
    def registerResourceOutputs(registerResourceOutputsRequest: RegisterResourceOutputsRequest): Result[Unit] =
      Result.fail(Exception("Not implemented"))
    def supportsFeature(supportsFeatureRequest: SupportsFeatureRequest): Result[SupportsFeatureResponse] =
      Result.fail(Exception("Not implemented"))
    def registerStackTransform(request: Callback): Result[Unit] =
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
      config       <- Config(runInfo.project, isProjectName = true, configMap = configMap, configSecretKeys = configSecretKeys)
      resources    <- Resources()
      memo         <- Memo()
      given Context = Context.create(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, resources, memo, stackPromise)
      _ <- stackPromise.fulfill(StackResource()(using ComponentBase(Output(besom.types.URN.empty))))
    yield summon[Context]

end DummyContext
