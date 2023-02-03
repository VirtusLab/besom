package besom.internal

import pulumirpc.resource.SupportsFeatureRequest

class FeatureSupport(monitor: Monitor, cacheRef: Ref[Map[String, Boolean]]):

  def isFeatureSupported(feature: String): Result[Boolean] = cacheRef.get.flatMap { cache =>
    cache.get(feature).map(Result.pure).getOrElse {
      val request = SupportsFeatureRequest(feature)

      monitor.supportsFeature(request).flatMap { response =>
        cacheRef.update(_.updated(feature, response.hasSupport)) *> Result.pure(response.hasSupport)
      }
    }
  }
