package besom.auto.internal

import besom.json.*

// ── Enums ──────────────────────────────────────────────────────────────

/** Describes the kind of difference between two property values. */
enum DiffKind(val value: String):
  case Add extends DiffKind("add")
  case AddReplace extends DiffKind("add-replace")
  case Delete extends DiffKind("delete")
  case DeleteReplace extends DiffKind("delete-replace")
  case Update extends DiffKind("update")
  case UpdateReplace extends DiffKind("update-replace")

  def forcesReplacement: Boolean = value.endsWith("-replace")
end DiffKind
object DiffKind:
  def from(value: String): DiffKind = value match
    case "add"            => DiffKind.Add
    case "add-replace"    => DiffKind.AddReplace
    case "delete"         => DiffKind.Delete
    case "delete-replace" => DiffKind.DeleteReplace
    case "update"         => DiffKind.Update
    case "update-replace" => DiffKind.UpdateReplace
    case other            => throw DeserializationException(s"Unknown DiffKind: $other")

  given RootJsonFormat[DiffKind] with
    def write(obj: DiffKind): JsValue = JsString(obj.value)
    def read(json: JsValue): DiffKind = json match
      case JsString(s) => DiffKind.from(s)
      case _           => throw DeserializationException("Expected string for DiffKind")
end DiffKind

/** Describes the type of progress event. */
enum ProgressType(val value: String):
  case PluginDownload extends ProgressType("plugin-download")
  case PluginInstall extends ProgressType("plugin-install")
end ProgressType
object ProgressType:
  def from(value: String): ProgressType = value match
    case "plugin-download" => ProgressType.PluginDownload
    case "plugin-install"  => ProgressType.PluginInstall
    case other             => throw DeserializationException(s"Unknown ProgressType: $other")

  given RootJsonFormat[ProgressType] with
    def write(obj: ProgressType): JsValue = JsString(obj.value)
    def read(json: JsValue): ProgressType = json match
      case JsString(s) => ProgressType.from(s)
      case _           => throw DeserializationException("Expected string for ProgressType")
end ProgressType

// ── Supporting types ───────────────────────────────────────────────────

/** Describes the difference between a property's old and new values.
  *
  * All fields required per Go source (no omitempty).
  */
case class PropertyDiff(
  diffKind: DiffKind,
  inputDiff: Boolean
) derives JsonFormat

/** Resource state as part of a step event.
  *
  * Required fields (always present in JSON): type, urn, id, parent, inputs, outputs, provider. Optional fields (omitempty in Go): custom,
  * delete, protect, retainOnDelete, initErrors.
  */
case class StepEventStateMetadata(
  `type`: String,
  urn: String,
  id: String,
  parent: String,
  inputs: Option[Map[String, JsValue]],
  outputs: Option[Map[String, JsValue]],
  provider: String,
  custom: Boolean = false,
  delete: Boolean = false,
  protect: Boolean = false,
  retainOnDelete: Boolean = false,
  initErrors: Option[List[String]] = None
) derives JsonFormat

/** Step event metadata including operation type, resource URN, and old/new state.
  *
  * Uses backtick-escaped `` `new` `` field to match JSON key `"new"`.
  *
  * Required: op, urn, type, old (nullable), new (nullable), detailedDiff (nullable), provider. Optional (omitempty): keys, diffs, logical.
  */
case class StepEventMetadata(
  op: OpType,
  urn: String,
  `type`: String,
  provider: String,
  old: Option[StepEventStateMetadata],
  `new`: Option[StepEventStateMetadata],
  detailedDiff: Option[Map[String, PropertyDiff]],
  keys: Option[List[String]] = None,
  diffs: Option[List[String]] = None,
  logical: Boolean = false
) derives JsonFormat

// ── Event types ────────────────────────────────────────────────────────

/** Emitted when the user cancels the update or the update completes. */
case class CancelEvent() derives JsonFormat

/** Stdout messages from the engine. All fields required. */
case class StdoutEngineEvent(
  message: String,
  color: String
) derives JsonFormat

/** Emitted at the start of an operation. Config is always present. */
case class PreludeEvent(
  config: Map[String, String]
) derives JsonFormat

/** A diagnostic message from the Pulumi engine or a provider.
  *
  * Required: severity, message, color. Optional (omitempty): urn, prefix, streamID, ephemeral.
  */
case class DiagnosticEvent(
  severity: String,
  message: String,
  color: String,
  urn: Option[String] = None,
  prefix: Option[String] = None,
  streamID: Option[Int] = None,
  ephemeral: Boolean = false
) derives JsonFormat

/** Emitted before a resource is modified. Metadata is required, planning is omitempty. */
case class ResourcePreEvent(
  metadata: StepEventMetadata,
  planning: Boolean = false
) derives JsonFormat

/** Emitted when a resource operation completes and outputs are available. */
case class ResOutputsEvent(
  metadata: StepEventMetadata,
  planning: Boolean = false
) derives JsonFormat

/** Emitted when a resource operation fails. All fields required. */
case class ResOpFailedEvent(
  metadata: StepEventMetadata,
  status: Int,
  steps: Int
) derives JsonFormat

/** Emitted when a policy violation occurs.
  *
  * Required: message, color, policyName, policyPackName, policyPackVersion, policyPackVersionTag, enforcementLevel. Optional (omitempty):
  * resourceUrn, severity.
  */
case class PolicyEvent(
  message: String,
  color: String,
  policyName: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  enforcementLevel: String,
  resourceUrn: Option[String] = None,
  severity: Option[String] = None
) derives JsonFormat

/** Emitted during policy remediation.
  *
  * Required: color, policyName, policyPackName, policyPackVersion, policyPackVersionTag. Optional (omitempty): resourceUrn, before, after.
  */
case class PolicyRemediationEvent(
  color: String,
  policyName: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  resourceUrn: Option[String] = None,
  before: Option[Map[String, JsValue]] = None,
  after: Option[Map[String, JsValue]] = None
) derives JsonFormat

/** Emitted when a policy pack is loaded (empty event, signals load happened). */
case class PolicyLoadEvent() derives JsonFormat

/** Summary of policy analysis for a resource.
  *
  * Required: resourceUrn, policyPackName, policyPackVersion, policyPackVersionTag. Optional (omitempty): passed, failed.
  */
case class PolicyAnalyzeSummaryEvent(
  resourceUrn: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  passed: List[String] = Nil,
  failed: List[String] = Nil
) derives JsonFormat

/** Summary of policy remediation for a resource. */
case class PolicyRemediateSummaryEvent(
  resourceUrn: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  passed: List[String] = Nil,
  failed: List[String] = Nil
) derives JsonFormat

/** Stack-level summary of policy analysis. */
case class PolicyAnalyzeStackSummaryEvent(
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  passed: List[String] = Nil,
  failed: List[String] = Nil
) derives JsonFormat

/** Emitted when a debugging session starts. Config is omitempty. */
case class StartDebuggingEvent(
  config: Map[String, JsValue] = Map.empty
) derives JsonFormat

/** Emitted for plugin download/install progress.
  *
  * Note: `received` matches the Go JSON tag `"received"` (Go field is named `Completed`).
  */
case class ProgressEvent(
  `type`: ProgressType,
  id: String,
  message: String,
  received: Long,
  total: Long,
  done: Boolean
) derives JsonFormat

/** Emitted when an internal engine error occurs. Error field is required. */
case class ErrorEvent(
  error: String
) derives JsonFormat
