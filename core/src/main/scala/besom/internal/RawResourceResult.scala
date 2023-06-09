package besom.internal

import com.google.protobuf.struct.Struct

case class RawResourceResult(urn: String, id: Option[String], data: Struct, dependencies: Map[String, Set[Resource]])

object RawResourceResult:
  def fromResponse(response: pulumirpc.resource.RegisterResourceResponse)(using Context): Result[RawResourceResult] =
    Result {
      RawResourceResult(
        urn = response.urn,
        id = if response.id.isEmpty then None else Some(response.id),
        data = response.`object`.getOrElse {
          throw new Exception("ONIXPECTED: no object in response") // TODO is this correct?
        },
        dependencies = response.propertyDependencies.map { case (propertyName, propertyDeps) =>
          val deps: Set[Resource] = propertyDeps.urns.toSet
            .map(Output(_))
            .map(DependencyResource(_)) // we do not register DependencyResources!

          propertyName -> deps
        }
      )
    }
