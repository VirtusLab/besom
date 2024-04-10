package besom.internal

import pulumirpc.resource.SupportsFeatureRequest

case class FeatureSupport(
  keepResources: Boolean,
  keepOutputValues: Boolean,
  deletedWith: Boolean,
  aliasSpecs: Boolean,
  transforms: Boolean
)

object FeatureSupport:
  def apply(monitor: Monitor): Result[FeatureSupport] =
    for
      keepResources    <- monitor.supportsFeature(SupportsFeatureRequest("resourceReferences")).map(_.hasSupport)
      keepOutputValues <- monitor.supportsFeature(SupportsFeatureRequest("outputValues")).map(_.hasSupport)
      deletedWith      <- monitor.supportsFeature(SupportsFeatureRequest("deletedWith")).map(_.hasSupport)
      aliasSpecs       <- monitor.supportsFeature(SupportsFeatureRequest("aliasSpecs")).map(_.hasSupport)
      transforms       <- monitor.supportsFeature(SupportsFeatureRequest("transforms")).map(_.hasSupport)
    yield FeatureSupport(keepResources, keepOutputValues, deletedWith, aliasSpecs, transforms)
