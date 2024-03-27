package besom

import besom.internal.ResourceOptsVariant

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
  type Logger         = besom.internal.logging.UserLoggerFactory
  type NonEmptyString = besom.util.NonEmptyString
  object NonEmptyString extends besom.util.NonEmptyStringFactory
  type Decoder[A]                                    = besom.internal.Decoder[A]
  type Encoder[A]                                    = besom.internal.Encoder[A]
  type ArgsEncoder[A]                                = besom.internal.ArgsEncoder[A]
  type ProviderArgsEncoder[A]                        = besom.internal.ProviderArgsEncoder[A]
  type ResourceDecoder[A <: besom.internal.Resource] = besom.internal.ResourceDecoder[A]
  type ProviderResource                              = besom.internal.ProviderResource
  type CustomResource                                = besom.internal.CustomResource
  type RemoteComponentResource                       = besom.internal.RemoteComponentResource
  type CustomResourceOptions                         = besom.internal.CustomResourceOptions
  type ComponentResourceOptions                      = besom.internal.ComponentResourceOptions
  object CustomResourceOptions extends besom.internal.CustomResourceOptionsFactory
  object ComponentResourceOptions extends besom.internal.ComponentResourceOptionsFactory
  type ComponentBase                                      = besom.internal.ComponentBase
  type ComponentResource                                  = besom.internal.ComponentResource
  type RegistersOutputs[A <: ComponentResource & Product] = besom.internal.RegistersOutputs[A]
  type StackReference                                     = besom.internal.StackReference
  object StackReference extends besom.internal.StackReferenceFactory
  type StackReferenceArgs = besom.internal.StackReferenceArgs
  object StackReferenceArgs extends besom.internal.StackReferenceArgsFactory
  type StackReferenceResourceOptions = besom.internal.StackReferenceResourceOptions
  object StackReferenceResourceOptions extends besom.internal.StackReferenceResourceOptionsFactory
  type Stack = besom.internal.Stack
  object Stack extends besom.internal.StackFactory
  type ResourceCompanion[A <: besom.internal.Resource] = besom.internal.ResourceCompanion[A]
  object ResourceOptsVariant:
    type StackRef  = besom.internal.ResourceOptsVariant.StackRef
    type Custom    = besom.internal.ResourceOptsVariant.Custom
    type Component = besom.internal.ResourceOptsVariant.Component
    object StackRef extends besom.internal.ResourceOptsVariant.StackRef
    object Custom extends besom.internal.ResourceOptsVariant.Custom
    object Component extends besom.internal.ResourceOptsVariant.Component
  type CustomTimeouts = besom.internal.CustomTimeouts

  export besom.internal.InvokeOptions
end aliases
