package besom.util

import besom.json.*
import besom.internal.{Output, Context}
import besom.internal.Constants, Constants.SpecialSig

object JsonReaderInstances:
  implicit def outputJsonReader[A](using jsonReader: JsonReader[A], ctx: Context): JsonReader[Output[A]] =
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

            case _ => Output.fail(Exception("Invalid JSON"))

        case _ => Output.fail(Exception("Invalid JSON"))
