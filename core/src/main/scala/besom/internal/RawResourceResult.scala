package besom.internal

import com.google.protobuf.struct.*
import besom.types.*

case class RawResourceResult(urn: URN, id: Option[ResourceId], data: Struct, dependencies: Map[String, Set[Resource]])

object RawResourceResult:
  def fromResponse(response: pulumirpc.resource.ReadResourceResponse, id: ResourceId)(using Context): Result[RawResourceResult] =
    Result.evalTry(URN.from(response.urn)).map { urn =>
      RawResourceResult(
        urn = urn,
        id = Some(id),
        data = response.properties.getOrElse {
          throw new Exception("ONIXPECTED: no properties in ReadResourceResponse") // TODO is this correct?
        },
        dependencies = Map.empty
      )
    }

  def fromResponse(response: pulumirpc.resource.RegisterResourceResponse)(using Context): Result[RawResourceResult] =
    val dependenciesPerField =
      Result.sequenceMap {
        response.propertyDependencies
          .map { case (propertyName, propertyDeps) =>
            val urnsResult: Result[Set[URN]] = Result.sequence {
              propertyDeps.urns.toSet
                .map(urnString => Result.evalTry(URN.from(urnString)))
            }

            val depsForProperty: Result[Set[Resource]] = urnsResult.map { setOfUrns =>
              setOfUrns
                .map(Output(_))
                .map(DependencyResource(_)) // we do not register DependencyResources!
            }

            propertyName -> depsForProperty
          }
      }

    val urnResult = Result.evalTry(URN.from(response.urn))

    for
      deps <- dependenciesPerField
      urn  <- urnResult
    yield RawResourceResult(
      urn = urn,
      id = if response.id.isEmpty then None else Some(ResourceId.unsafeOf(response.id)),
      data = response.`object`.getOrElse {
        throw new Exception("ONIXPECTED: no object in response") // TODO is this correct?
      },
      dependencies = deps
    )

  def fromValue(tok: FunctionToken, value: Value)(using Context): Result[RawResourceResult] =
    value match
      case Value(Value.Kind.StructValue(struct), _) =>
        lazy val missingUrnErr =
          Result.fail(Exception(s"Expected invoke $tok to return a struct with a urn field but it's missing:\n${pprint(struct)}"))

        lazy val urnNotStringErr =
          Result.fail(Exception(s"Expected invoke $tok response to contain a valid URN, got\n${pprint(struct)}"))

        val urnResult =
          struct.fields
            .get(Constants.UrnPropertyName)
            .fold(missingUrnErr) { value =>
              if value.kind.isStringValue then Result.evalTry(URN.from(value.getStringValue))
              else urnNotStringErr
            }

        lazy val idNotStringErr =
          Result.fail(Exception(s"Expected invoke $tok response to contain a valid ID, got\n${pprint(struct)}"))

        lazy val missingId = Result.pure(None)

        val idResult = struct.fields.get(Constants.IdPropertyName).fold(missingId) { value =>
          if value.kind.isStringValue then Result(Some(ResourceId.unsafeOf(value.getStringValue)))
          else idNotStringErr
        }

        lazy val missingStateErr =
          Result.fail(Exception(s"Expected invoke $tok to return a struct with a state field but it's missing:\n${pprint(struct)}"))

        lazy val stateNotStructErr =
          Result.fail(Exception(s"Expected invoke $tok response to contain a valid state, got\n${pprint(struct)}"))

        val stateResult = struct.fields.get(Constants.StatePropertyName).fold(missingStateErr) { value =>
          if value.kind.isStructValue then Result(value.getStructValue)
          else stateNotStructErr
        }

        for
          urn     <- urnResult
          maybeId <- idResult
          state   <- stateResult
        yield RawResourceResult(
          urn = urn,
          id = maybeId,
          data = state,
          dependencies = Map.empty
        )

      case differentResult =>
        Result.fail(Exception(s"Expected struct value from invoke of $tok, got ${pprint(differentResult)}"))

end RawResourceResult
