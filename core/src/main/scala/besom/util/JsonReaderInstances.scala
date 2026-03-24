package besom.util

import besom.json.*
import besom.internal.Output
import besom.internal.Constants, Constants.SpecialSig

object JsonReaderInstances:
  implicit def outputJsonReader[A](using jsonReader: JsonReader[A]): JsonReader[Output[A]] =
    new JsonReader[Output[A]]:
      def read(json: JsValue): Output[A] = json match
        case JsObject(fields) =>
          fields.get(SpecialSig.Key) match
            case Some(JsString(sig)) if SpecialSig.fromString(sig) == Some(SpecialSig.OutputSig) =>
              val maybeInnerValue = fields.get(Constants.ValueName)
              maybeInnerValue
                .map { innerValue =>
                  try Output.pure(jsonReader.read(innerValue))
                  catch case e: Throwable => Output.fail(e)
                }
                .getOrElse(Output.fail(Exception("Invalid JSON")))

            case Some(JsString(sig)) if SpecialSig.fromString(sig) == Some(SpecialSig.SecretSig) =>
              val maybeInnerValue = fields.get(Constants.ValueName)
              maybeInnerValue
                .map { innerValue =>
                  try Output.secret(jsonReader.read(innerValue))
                  catch case e: Throwable => Output.fail(e)
                }
                .getOrElse(Output.fail(Exception("Invalid JSON")))

            case _ =>
              // Regular JsObject without envelope — read as plain value wrapped in Output
              try Output.pure(jsonReader.read(json))
              catch case e: Throwable => Output.fail(e)

        // Plain value (not a JsObject) — read directly and wrap in Output.
        // This handles values that have already been stripped of secret envelopes.
        case _ =>
          try Output.pure(jsonReader.read(json))
          catch case e: Throwable => Output.fail(e)
