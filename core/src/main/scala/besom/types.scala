package besom

import besom.internal.{Decoder, Encoder}

object types:
  // TODO: replace these stubs with proper implementations
  private object Opaques:
    opaque type PulumiAny = spray.json.JsValue
    object PulumiAny:
      given Encoder[PulumiAny] = Encoder.jsonEncoder
      given Decoder[PulumiAny] = Decoder.jsonDecoder

    opaque type PulumiJson = spray.json.JsValue
    object PulumiJson:
      given Encoder[PulumiJson] = Encoder.jsonEncoder
      given Decoder[PulumiJson] = Decoder.jsonDecoder

  export Opaques.*
