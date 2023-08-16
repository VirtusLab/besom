package besom.internal

import com.google.protobuf.struct.Struct
import besom.types.*

case class RawResourceResult(urn: URN, id: Option[ResourceId], data: Struct, dependencies: Map[String, Set[Resource]])

object RawResourceResult:
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
      id = if response.id.isEmpty then None else Some(ResourceId(response.id)),
      data = response.`object`.getOrElse {
        throw new Exception("ONIXPECTED: no object in response") // TODO is this correct?
      },
      dependencies = deps
    )
