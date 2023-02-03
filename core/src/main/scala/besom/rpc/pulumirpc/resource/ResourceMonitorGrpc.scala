// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.resource


object ResourceMonitorGrpc {
  val METHOD_SUPPORTS_FEATURE: _root_.io.grpc.MethodDescriptor[pulumirpc.resource.SupportsFeatureRequest, pulumirpc.resource.SupportsFeatureResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.ResourceMonitor", "SupportsFeature"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.SupportsFeatureRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.SupportsFeatureResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0).getMethods().get(0)))
      .build()
  
  val METHOD_INVOKE: _root_.io.grpc.MethodDescriptor[pulumirpc.resource.ResourceInvokeRequest, pulumirpc.provider.InvokeResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.ResourceMonitor", "Invoke"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.ResourceInvokeRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.provider.InvokeResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0).getMethods().get(1)))
      .build()
  
  val METHOD_STREAM_INVOKE: _root_.io.grpc.MethodDescriptor[pulumirpc.resource.ResourceInvokeRequest, pulumirpc.provider.InvokeResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.ResourceMonitor", "StreamInvoke"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.ResourceInvokeRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.provider.InvokeResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0).getMethods().get(2)))
      .build()
  
  val METHOD_CALL: _root_.io.grpc.MethodDescriptor[pulumirpc.provider.CallRequest, pulumirpc.provider.CallResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.ResourceMonitor", "Call"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.provider.CallRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.provider.CallResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0).getMethods().get(3)))
      .build()
  
  val METHOD_READ_RESOURCE: _root_.io.grpc.MethodDescriptor[pulumirpc.resource.ReadResourceRequest, pulumirpc.resource.ReadResourceResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.ResourceMonitor", "ReadResource"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.ReadResourceRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.ReadResourceResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0).getMethods().get(4)))
      .build()
  
  val METHOD_REGISTER_RESOURCE: _root_.io.grpc.MethodDescriptor[pulumirpc.resource.RegisterResourceRequest, pulumirpc.resource.RegisterResourceResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.ResourceMonitor", "RegisterResource"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.RegisterResourceRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.RegisterResourceResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0).getMethods().get(5)))
      .build()
  
  val METHOD_REGISTER_RESOURCE_OUTPUTS: _root_.io.grpc.MethodDescriptor[pulumirpc.resource.RegisterResourceOutputsRequest, com.google.protobuf.empty.Empty] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pulumirpc.ResourceMonitor", "RegisterResourceOutputs"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pulumirpc.resource.RegisterResourceOutputsRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[com.google.protobuf.empty.Empty])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0).getMethods().get(6)))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("pulumirpc.ResourceMonitor")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(pulumirpc.resource.ResourceProto.javaDescriptor))
      .addMethod(METHOD_SUPPORTS_FEATURE)
      .addMethod(METHOD_INVOKE)
      .addMethod(METHOD_STREAM_INVOKE)
      .addMethod(METHOD_CALL)
      .addMethod(METHOD_READ_RESOURCE)
      .addMethod(METHOD_REGISTER_RESOURCE)
      .addMethod(METHOD_REGISTER_RESOURCE_OUTPUTS)
      .build()
  
  /** ResourceMonitor is the interface a source uses to talk back to the planning monitor orchestrating the execution.
    */
  trait ResourceMonitor extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = ResourceMonitor
    def supportsFeature(request: pulumirpc.resource.SupportsFeatureRequest): scala.concurrent.Future[pulumirpc.resource.SupportsFeatureResponse]
    def invoke(request: pulumirpc.resource.ResourceInvokeRequest): scala.concurrent.Future[pulumirpc.provider.InvokeResponse]
    def streamInvoke(request: pulumirpc.resource.ResourceInvokeRequest, responseObserver: _root_.io.grpc.stub.StreamObserver[pulumirpc.provider.InvokeResponse]): _root_.scala.Unit
    def call(request: pulumirpc.provider.CallRequest): scala.concurrent.Future[pulumirpc.provider.CallResponse]
    def readResource(request: pulumirpc.resource.ReadResourceRequest): scala.concurrent.Future[pulumirpc.resource.ReadResourceResponse]
    def registerResource(request: pulumirpc.resource.RegisterResourceRequest): scala.concurrent.Future[pulumirpc.resource.RegisterResourceResponse]
    def registerResourceOutputs(request: pulumirpc.resource.RegisterResourceOutputsRequest): scala.concurrent.Future[com.google.protobuf.empty.Empty]
  }
  
  object ResourceMonitor extends _root_.scalapb.grpc.ServiceCompanion[ResourceMonitor] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[ResourceMonitor] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.ServiceDescriptor = pulumirpc.resource.ResourceProto.scalaDescriptor.services(0)
    def bindService(serviceImpl: ResourceMonitor, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
      _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
      .addMethod(
        METHOD_SUPPORTS_FEATURE,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.resource.SupportsFeatureRequest, pulumirpc.resource.SupportsFeatureResponse] {
          override def invoke(request: pulumirpc.resource.SupportsFeatureRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.resource.SupportsFeatureResponse]): _root_.scala.Unit =
            serviceImpl.supportsFeature(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .addMethod(
        METHOD_INVOKE,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.resource.ResourceInvokeRequest, pulumirpc.provider.InvokeResponse] {
          override def invoke(request: pulumirpc.resource.ResourceInvokeRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.provider.InvokeResponse]): _root_.scala.Unit =
            serviceImpl.invoke(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .addMethod(
        METHOD_STREAM_INVOKE,
        _root_.io.grpc.stub.ServerCalls.asyncServerStreamingCall(new _root_.io.grpc.stub.ServerCalls.ServerStreamingMethod[pulumirpc.resource.ResourceInvokeRequest, pulumirpc.provider.InvokeResponse] {
          override def invoke(request: pulumirpc.resource.ResourceInvokeRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.provider.InvokeResponse]): _root_.scala.Unit =
            serviceImpl.streamInvoke(request, observer)
        }))
      .addMethod(
        METHOD_CALL,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.provider.CallRequest, pulumirpc.provider.CallResponse] {
          override def invoke(request: pulumirpc.provider.CallRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.provider.CallResponse]): _root_.scala.Unit =
            serviceImpl.call(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .addMethod(
        METHOD_READ_RESOURCE,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.resource.ReadResourceRequest, pulumirpc.resource.ReadResourceResponse] {
          override def invoke(request: pulumirpc.resource.ReadResourceRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.resource.ReadResourceResponse]): _root_.scala.Unit =
            serviceImpl.readResource(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .addMethod(
        METHOD_REGISTER_RESOURCE,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.resource.RegisterResourceRequest, pulumirpc.resource.RegisterResourceResponse] {
          override def invoke(request: pulumirpc.resource.RegisterResourceRequest, observer: _root_.io.grpc.stub.StreamObserver[pulumirpc.resource.RegisterResourceResponse]): _root_.scala.Unit =
            serviceImpl.registerResource(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .addMethod(
        METHOD_REGISTER_RESOURCE_OUTPUTS,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pulumirpc.resource.RegisterResourceOutputsRequest, com.google.protobuf.empty.Empty] {
          override def invoke(request: pulumirpc.resource.RegisterResourceOutputsRequest, observer: _root_.io.grpc.stub.StreamObserver[com.google.protobuf.empty.Empty]): _root_.scala.Unit =
            serviceImpl.registerResourceOutputs(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .build()
  }
  
  /** ResourceMonitor is the interface a source uses to talk back to the planning monitor orchestrating the execution.
    */
  trait ResourceMonitorBlockingClient {
    def serviceCompanion = ResourceMonitor
    def supportsFeature(request: pulumirpc.resource.SupportsFeatureRequest): pulumirpc.resource.SupportsFeatureResponse
    def invoke(request: pulumirpc.resource.ResourceInvokeRequest): pulumirpc.provider.InvokeResponse
    def streamInvoke(request: pulumirpc.resource.ResourceInvokeRequest): scala.collection.Iterator[pulumirpc.provider.InvokeResponse]
    def call(request: pulumirpc.provider.CallRequest): pulumirpc.provider.CallResponse
    def readResource(request: pulumirpc.resource.ReadResourceRequest): pulumirpc.resource.ReadResourceResponse
    def registerResource(request: pulumirpc.resource.RegisterResourceRequest): pulumirpc.resource.RegisterResourceResponse
    def registerResourceOutputs(request: pulumirpc.resource.RegisterResourceOutputsRequest): com.google.protobuf.empty.Empty
  }
  
  class ResourceMonitorBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[ResourceMonitorBlockingStub](channel, options) with ResourceMonitorBlockingClient {
    override def supportsFeature(request: pulumirpc.resource.SupportsFeatureRequest): pulumirpc.resource.SupportsFeatureResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_SUPPORTS_FEATURE, options, request)
    }
    
    override def invoke(request: pulumirpc.resource.ResourceInvokeRequest): pulumirpc.provider.InvokeResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_INVOKE, options, request)
    }
    
    override def streamInvoke(request: pulumirpc.resource.ResourceInvokeRequest): scala.collection.Iterator[pulumirpc.provider.InvokeResponse] = {
      _root_.scalapb.grpc.ClientCalls.blockingServerStreamingCall(channel, METHOD_STREAM_INVOKE, options, request)
    }
    
    override def call(request: pulumirpc.provider.CallRequest): pulumirpc.provider.CallResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_CALL, options, request)
    }
    
    override def readResource(request: pulumirpc.resource.ReadResourceRequest): pulumirpc.resource.ReadResourceResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_READ_RESOURCE, options, request)
    }
    
    override def registerResource(request: pulumirpc.resource.RegisterResourceRequest): pulumirpc.resource.RegisterResourceResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_REGISTER_RESOURCE, options, request)
    }
    
    override def registerResourceOutputs(request: pulumirpc.resource.RegisterResourceOutputsRequest): com.google.protobuf.empty.Empty = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_REGISTER_RESOURCE_OUTPUTS, options, request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): ResourceMonitorBlockingStub = new ResourceMonitorBlockingStub(channel, options)
  }
  
  class ResourceMonitorStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[ResourceMonitorStub](channel, options) with ResourceMonitor {
    override def supportsFeature(request: pulumirpc.resource.SupportsFeatureRequest): scala.concurrent.Future[pulumirpc.resource.SupportsFeatureResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_SUPPORTS_FEATURE, options, request)
    }
    
    override def invoke(request: pulumirpc.resource.ResourceInvokeRequest): scala.concurrent.Future[pulumirpc.provider.InvokeResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_INVOKE, options, request)
    }
    
    override def streamInvoke(request: pulumirpc.resource.ResourceInvokeRequest, responseObserver: _root_.io.grpc.stub.StreamObserver[pulumirpc.provider.InvokeResponse]): _root_.scala.Unit = {
      _root_.scalapb.grpc.ClientCalls.asyncServerStreamingCall(channel, METHOD_STREAM_INVOKE, options, request, responseObserver)
    }
    
    override def call(request: pulumirpc.provider.CallRequest): scala.concurrent.Future[pulumirpc.provider.CallResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_CALL, options, request)
    }
    
    override def readResource(request: pulumirpc.resource.ReadResourceRequest): scala.concurrent.Future[pulumirpc.resource.ReadResourceResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_READ_RESOURCE, options, request)
    }
    
    override def registerResource(request: pulumirpc.resource.RegisterResourceRequest): scala.concurrent.Future[pulumirpc.resource.RegisterResourceResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_REGISTER_RESOURCE, options, request)
    }
    
    override def registerResourceOutputs(request: pulumirpc.resource.RegisterResourceOutputsRequest): scala.concurrent.Future[com.google.protobuf.empty.Empty] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_REGISTER_RESOURCE_OUTPUTS, options, request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): ResourceMonitorStub = new ResourceMonitorStub(channel, options)
  }
  
  object ResourceMonitorStub extends _root_.io.grpc.stub.AbstractStub.StubFactory[ResourceMonitorStub] {
    override def newStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): ResourceMonitorStub = new ResourceMonitorStub(channel, options)
    
    implicit val stubFactory: _root_.io.grpc.stub.AbstractStub.StubFactory[ResourceMonitorStub] = this
  }
  
  def bindService(serviceImpl: ResourceMonitor, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition = ResourceMonitor.bindService(serviceImpl, executionContext)
  
  def blockingStub(channel: _root_.io.grpc.Channel): ResourceMonitorBlockingStub = new ResourceMonitorBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): ResourceMonitorStub = new ResourceMonitorStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = pulumirpc.resource.ResourceProto.javaDescriptor.getServices().get(0)
  
}