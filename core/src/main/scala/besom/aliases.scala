package besom

object aliases:
  type Output[+A] = besom.internal.Output[A]
  object Output extends besom.internal.OutputFactory
  object OutputExtensions extends besom.internal.OutputExtensionsFactory
  export OutputExtensions.*
  type Input[+A] = besom.internal.Input[A]
  object Input:
    export besom.internal.Input.*
  type Context = besom.internal.Context
  export besom.internal.logging.MDC
  type Config = besom.internal.Config
  object Config extends besom.internal.ConfigFactory
  type Logger = besom.internal.logging.UserLoggerFactory
  type NonEmptyString = besom.util.NonEmptyString
  object NonEmptyString extends besom.util.NonEmptyStringFactory
  type Decoder[A] = besom.internal.Decoder[A]
  type Encoder[A] = besom.internal.Encoder[A]
  type ArgsEncoder[A] = besom.internal.ArgsEncoder[A]
  type ProviderArgsEncoder[A] = besom.internal.ProviderArgsEncoder[A]
  type ResourceDecoder[A <: besom.internal.Resource] = besom.internal.ResourceDecoder[A]
  type ProviderResource = besom.internal.ProviderResource
  type CustomResource = besom.internal.CustomResource
  type CustomResourceOptions = besom.internal.CustomResourceOptions
  object CustomResourceOptions extends besom.internal.CustomResourceOptionsFactory
  type ComponentBase = besom.internal.ComponentBase
  type ComponentResource = besom.internal.ComponentResource
  type RegistersOutputs[A <: ComponentResource & Product] = besom.internal.RegistersOutputs[A]
  export besom.internal.InvokeOptions
