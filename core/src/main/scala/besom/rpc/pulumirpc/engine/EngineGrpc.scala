// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.engine


object EngineGrpc {
  val METHOD_LOG: _root_.io.grpc.MethodDescriptor[pulumirpc.engine.LogRequest, com.google.protobuf.empty.Empty] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.Engine", "Log"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.engine.LogRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[com.google.protobuf.empty.Empty])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.engine.EngineProto.javaDescriptor.getServices().get(0).getMethods().get(0)))
      .build()
  
  val METHOD_GET_ROOT_RESOURCE: _root_.io.grpc.MethodDescriptor[pulumirpc.engine.GetRootResourceRequest, pulumirpc.engine.GetRootResourceResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.Engine", "GetRootResource"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.engine.GetRootResourceRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.engine.GetRootResourceResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.engine.EngineProto.javaDescriptor.getServices().get(0).getMethods().get(1)))
      .build()
  
  val METHOD_SET_ROOT_RESOURCE: _root_.io.grpc.MethodDescriptor[pulumirpc.engine.SetRootResourceRequest, pulumirpc.engine.SetRootResourceResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.Engine", "SetRootResource"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.engine.SetRootResourceRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.engine.SetRootResourceResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.engine.EngineProto.javaDescriptor.getServices().get(0).getMethods().get(2)))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("pulumirpc.Engine")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(pulumirpc.engine.EngineProto.javaDescriptor))
      .addMethod(METHOD_LOG)
      .addMethod(METHOD_GET_ROOT_RESOURCE)
      .addMethod(METHOD_SET_ROOT_RESOURCE)
      .build()
  
  /** Engine is an auxiliary service offered to language and resource provider plugins. Its main purpose today is
    * to serve as a common logging endpoint, but it also serves as a state storage mechanism for language hosts
    * that can't store their own global state.
    */
  trait Engine extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = Engine
    /** Log logs a global message in the engine, including errors and warnings.
      */
    def log(request: pulumirpc.engine.LogRequest): scala.concurrent.Future[com.google.protobuf.empty.Empty]
    /** GetRootResource gets the URN of the root resource, the resource that should be the root of all
      * otherwise-unparented resources.
      */
    def getRootResource(request: pulumirpc.engine.GetRootResourceRequest): scala.concurrent.Future[pulumirpc.engine.GetRootResourceResponse]
    /** SetRootResource sets the URN of the root resource.
      */
    def setRootResource(request: pulumirpc.engine.SetRootResourceRequest): scala.concurrent.Future[pulumirpc.engine.SetRootResourceResponse]
  }
  
  object Engine extends _root_.scalapb.grpc.ServiceCompanion[Engine] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[Engine] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = pulumirpc.engine.EngineProto.javaDescriptor.getServices().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.ServiceDescriptor = pulumirpc.engine.EngineProto.scalaDescriptor.services(0)
    def bindService(serviceImpl: Engine, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
      _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
      .addMethod(
        METHOD_LOG,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.engine.LogRequest, com.google.protobuf.empty.Empty] {
          override def invoke(request: pulumirpc.engine.LogRequest, observer: _root_.io.grpc.stub.StreamObserver[com.google.protobuf.empty.Empty]): _root_.scala.Unit =
            serviceImpl.log(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .addMethod(
        METHOD_GET_ROOT_RESOURCE,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.engine.GetRootResourceRequest, pulumirpc.engine.GetRootResourceResponse] {
          override def invoke(request: pulumirpc.engine.GetRootResourceRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.engine.GetRootResourceResponse]): _root_.scala.Unit =
            serviceImpl.getRootResource(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .addMethod(
        METHOD_SET_ROOT_RESOURCE,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.engine.SetRootResourceRequest, pulumirpc.engine.SetRootResourceResponse] {
          override def invoke(request: pulumirpc.engine.SetRootResourceRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.engine.SetRootResourceResponse]): _root_.scala.Unit =
            serviceImpl.setRootResource(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .build()
  }
  
  /** Engine is an auxiliary service offered to language and resource provider plugins. Its main purpose today is
    * to serve as a common logging endpoint, but it also serves as a state storage mechanism for language hosts
    * that can't store their own global state.
    */
  trait EngineBlockingClient {
    def serviceCompanion = Engine
    /** Log logs a global message in the engine, including errors and warnings.
      */
    def log(request: pulumirpc.engine.LogRequest): com.google.protobuf.empty.Empty
    /** GetRootResource gets the URN of the root resource, the resource that should be the root of all
      * otherwise-unparented resources.
      */
    def getRootResource(request: pulumirpc.engine.GetRootResourceRequest): pulumirpc.engine.GetRootResourceResponse
    /** SetRootResource sets the URN of the root resource.
      */
    def setRootResource(request: pulumirpc.engine.SetRootResourceRequest): pulumirpc.engine.SetRootResourceResponse
  }
  
  class EngineBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[EngineBlockingStub](channel, options) with EngineBlockingClient {
    /** Log logs a global message in the engine, including errors and warnings.
      */
    override def log(request: pulumirpc.engine.LogRequest): com.google.protobuf.empty.Empty = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_LOG, options, request)
    }
    
    /** GetRootResource gets the URN of the root resource, the resource that should be the root of all
      * otherwise-unparented resources.
      */
    override def getRootResource(request: pulumirpc.engine.GetRootResourceRequest): pulumirpc.engine.GetRootResourceResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_GET_ROOT_RESOURCE, options, request)
    }
    
    /** SetRootResource sets the URN of the root resource.
      */
    override def setRootResource(request: pulumirpc.engine.SetRootResourceRequest): pulumirpc.engine.SetRootResourceResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_SET_ROOT_RESOURCE, options, request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): EngineBlockingStub = new EngineBlockingStub(channel, options)
  }
  
  class EngineStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[EngineStub](channel, options) with Engine {
    /** Log logs a global message in the engine, including errors and warnings.
      */
    override def log(request: pulumirpc.engine.LogRequest): scala.concurrent.Future[com.google.protobuf.empty.Empty] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_LOG, options, request)
    }
    
    /** GetRootResource gets the URN of the root resource, the resource that should be the root of all
      * otherwise-unparented resources.
      */
    override def getRootResource(request: pulumirpc.engine.GetRootResourceRequest): scala.concurrent.Future[pulumirpc.engine.GetRootResourceResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_GET_ROOT_RESOURCE, options, request)
    }
    
    /** SetRootResource sets the URN of the root resource.
      */
    override def setRootResource(request: pulumirpc.engine.SetRootResourceRequest): scala.concurrent.Future[pulumirpc.engine.SetRootResourceResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_SET_ROOT_RESOURCE, options, request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): EngineStub = new EngineStub(channel, options)
  }
  
  object EngineStub extends _root_.io.grpc.stub.AbstractStub.StubFactory[EngineStub] {
    override def newStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): EngineStub = new EngineStub(channel, options)
    
    implicit val stubFactory: _root_.io.grpc.stub.AbstractStub.StubFactory[EngineStub] = this
  }
  
  def bindService(serviceImpl: Engine, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition = Engine.bindService(serviceImpl, executionContext)
  
  def blockingStub(channel: _root_.io.grpc.Channel): EngineBlockingStub = new EngineBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): EngineStub = new EngineStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = pulumirpc.engine.EngineProto.javaDescriptor.getServices().get(0)
  
}