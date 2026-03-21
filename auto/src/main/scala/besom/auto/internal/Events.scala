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

  implicit object DiffKindFormat extends RootJsonFormat[DiffKind] {
    def write(obj: DiffKind): JsValue = JsString(obj.value)
    def read(json: JsValue): DiffKind = json match
      case JsString(s) => DiffKind.from(s)
      case _           => throw DeserializationException("Expected string for DiffKind")
  }
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

  implicit object ProgressTypeFormat extends RootJsonFormat[ProgressType] {
    def write(obj: ProgressType): JsValue = JsString(obj.value)
    def read(json: JsValue): ProgressType = json match
      case JsString(s) => ProgressType.from(s)
      case _           => throw DeserializationException("Expected string for ProgressType")
  }
end ProgressType

// ── Supporting types ───────────────────────────────────────────────────

/** Describes the difference between a property's old and new values. */
case class PropertyDiff(
  diffKind: DiffKind,
  inputDiff: Boolean
)
object PropertyDiff:
  implicit object PropertyDiffFormat extends RootJsonFormat[PropertyDiff] {
    def write(obj: PropertyDiff): JsValue = ???
    def read(json: JsValue): PropertyDiff = {
      val obj = json.asJsObject
      PropertyDiff(
        diffKind = obj.fields.get("diffKind").map(_.convertTo[DiffKind]).getOrElse(DiffKind.Update),
        inputDiff = obj.fields.get("inputDiff").exists(_.convertTo[Boolean])
      )
    }
  }
end PropertyDiff

/** Resource state as part of a step event. */
case class StepEventStateMetadata(
  `type`: String,
  urn: String,
  id: String,
  parent: String,
  custom: Boolean,
  delete: Boolean,
  protect: Boolean,
  retainOnDelete: Boolean,
  inputs: Option[Map[String, JsValue]],
  outputs: Option[Map[String, JsValue]],
  provider: String,
  initErrors: Option[List[String]]
)
object StepEventStateMetadata:
  implicit object StepEventStateMetadataFormat extends RootJsonFormat[StepEventStateMetadata] {
    def write(obj: StepEventStateMetadata): JsValue = ???
    def read(json: JsValue): StepEventStateMetadata = {
      val obj    = json.asJsObject
      val fields = obj.fields
      StepEventStateMetadata(
        `type` = fields.get("type").map(_.convertTo[String]).getOrElse(""),
        urn = fields.get("urn").map(_.convertTo[String]).getOrElse(""),
        id = fields.get("id").map(_.convertTo[String]).getOrElse(""),
        parent = fields.get("parent").map(_.convertTo[String]).getOrElse(""),
        custom = fields.get("custom").exists(_.convertTo[Boolean]),
        delete = fields.get("delete").exists(_.convertTo[Boolean]),
        protect = fields.get("protect").exists(_.convertTo[Boolean]),
        retainOnDelete = fields.get("retainOnDelete").exists(_.convertTo[Boolean]),
        inputs = fields.get("inputs").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[Map[String, JsValue]])
        },
        outputs = fields.get("outputs").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[Map[String, JsValue]])
        },
        provider = fields.get("provider").map(_.convertTo[String]).getOrElse(""),
        initErrors = fields.get("initErrors").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[List[String]])
        }
      )
    }
  }
end StepEventStateMetadata

/** Step event metadata including operation type, resource URN, and old/new state. */
case class StepEventMetadata(
  op: OpType,
  urn: String,
  `type`: String,
  provider: String,
  old: Option[StepEventStateMetadata],
  new_ : Option[StepEventStateMetadata],
  keys: Option[List[String]],
  diffs: Option[List[String]],
  detailedDiff: Option[Map[String, PropertyDiff]],
  logical: Boolean
)
object StepEventMetadata:
  implicit object StepEventMetadataFormat extends RootJsonFormat[StepEventMetadata] {
    def write(obj: StepEventMetadata): JsValue = ???
    def read(json: JsValue): StepEventMetadata = {
      val obj    = json.asJsObject
      val fields = obj.fields
      StepEventMetadata(
        op = fields("op").convertTo[OpType],
        urn = fields.get("urn").map(_.convertTo[String]).getOrElse(""),
        `type` = fields.get("type").map(_.convertTo[String]).getOrElse(""),
        provider = fields.get("provider").map(_.convertTo[String]).getOrElse(""),
        old = fields.get("old").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[StepEventStateMetadata])
        },
        new_ = fields.get("new").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[StepEventStateMetadata])
        },
        keys = fields.get("keys").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[List[String]])
        },
        diffs = fields.get("diffs").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[List[String]])
        },
        detailedDiff = fields.get("detailedDiff").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[Map[String, PropertyDiff]])
        },
        logical = fields.get("logical").exists(_.convertTo[Boolean])
      )
    }
  }
end StepEventMetadata

// ── Event types ────────────────────────────────────────────────────────

/** Emitted when the user cancels the update or the update completes. */
case class CancelEvent()
object CancelEvent:
  implicit object CancelEventFormat extends RootJsonFormat[CancelEvent] {
    def write(obj: CancelEvent): JsValue = ???
    def read(json: JsValue): CancelEvent = CancelEvent()
  }
end CancelEvent

/** Stdout messages from the engine. */
case class StdoutEngineEvent(
  message: String,
  color: String
)
object StdoutEngineEvent:
  implicit object StdoutEngineEventFormat extends RootJsonFormat[StdoutEngineEvent] {
    def write(obj: StdoutEngineEvent): JsValue = ???
    def read(json: JsValue): StdoutEngineEvent = {
      val fields = json.asJsObject.fields
      StdoutEngineEvent(
        message = fields.get("message").map(_.convertTo[String]).getOrElse(""),
        color = fields.get("color").map(_.convertTo[String]).getOrElse("")
      )
    }
  }
end StdoutEngineEvent

/** Emitted at the start of an operation, contains config. */
case class PreludeEvent(
  config: Map[String, String]
)
object PreludeEvent:
  implicit object PreludeEventFormat extends RootJsonFormat[PreludeEvent] {
    def write(obj: PreludeEvent): JsValue = ???
    def read(json: JsValue): PreludeEvent = {
      val fields = json.asJsObject.fields
      PreludeEvent(
        config = fields.get("config").map(_.convertTo[Map[String, String]]).getOrElse(Map.empty)
      )
    }
  }
end PreludeEvent

/** A diagnostic message from the Pulumi engine or a provider. */
case class DiagnosticEvent(
  urn: Option[String],
  prefix: String,
  severity: String,
  message: String,
  color: String,
  streamID: Option[Int],
  ephemeral: Boolean
)
object DiagnosticEvent:
  implicit object DiagnosticEventFormat extends RootJsonFormat[DiagnosticEvent] {
    def write(obj: DiagnosticEvent): JsValue = ???
    def read(json: JsValue): DiagnosticEvent = {
      val fields = json.asJsObject.fields
      DiagnosticEvent(
        urn = fields.get("urn").map(_.convertTo[String]),
        prefix = fields.get("prefix").map(_.convertTo[String]).getOrElse(""),
        severity = fields.get("severity").map(_.convertTo[String]).getOrElse(""),
        message = fields.get("message").map(_.convertTo[String]).getOrElse(""),
        color = fields.get("color").map(_.convertTo[String]).getOrElse(""),
        streamID = fields.get("streamID").map(_.convertTo[Int]),
        ephemeral = fields.get("ephemeral").exists(_.convertTo[Boolean])
      )
    }
  }
end DiagnosticEvent

/** Emitted before a resource is modified. */
case class ResourcePreEvent(
  metadata: StepEventMetadata,
  planning: Boolean
)
object ResourcePreEvent:
  implicit object ResourcePreEventFormat extends RootJsonFormat[ResourcePreEvent] {
    def write(obj: ResourcePreEvent): JsValue = ???
    def read(json: JsValue): ResourcePreEvent = {
      val fields = json.asJsObject.fields
      ResourcePreEvent(
        metadata = fields("metadata").convertTo[StepEventMetadata],
        planning = fields.get("planning").exists(_.convertTo[Boolean])
      )
    }
  }
end ResourcePreEvent

/** Emitted when a resource operation completes and outputs are available. */
case class ResOutputsEvent(
  metadata: StepEventMetadata,
  planning: Boolean
)
object ResOutputsEvent:
  implicit object ResOutputsEventFormat extends RootJsonFormat[ResOutputsEvent] {
    def write(obj: ResOutputsEvent): JsValue = ???
    def read(json: JsValue): ResOutputsEvent = {
      val fields = json.asJsObject.fields
      ResOutputsEvent(
        metadata = fields("metadata").convertTo[StepEventMetadata],
        planning = fields.get("planning").exists(_.convertTo[Boolean])
      )
    }
  }
end ResOutputsEvent

/** Emitted when a resource operation fails. */
case class ResOpFailedEvent(
  metadata: StepEventMetadata,
  status: Int,
  steps: Int
)
object ResOpFailedEvent:
  implicit object ResOpFailedEventFormat extends RootJsonFormat[ResOpFailedEvent] {
    def write(obj: ResOpFailedEvent): JsValue = ???
    def read(json: JsValue): ResOpFailedEvent = {
      val fields = json.asJsObject.fields
      ResOpFailedEvent(
        metadata = fields("metadata").convertTo[StepEventMetadata],
        status = fields.get("status").map(_.convertTo[Int]).getOrElse(0),
        steps = fields.get("steps").map(_.convertTo[Int]).getOrElse(0)
      )
    }
  }
end ResOpFailedEvent

/** Emitted when a policy violation occurs. */
case class PolicyEvent(
  resourceUrn: Option[String],
  message: String,
  color: String,
  policyName: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  enforcementLevel: String,
  severity: String
)
object PolicyEvent:
  implicit object PolicyEventFormat extends RootJsonFormat[PolicyEvent] {
    def write(obj: PolicyEvent): JsValue = ???
    def read(json: JsValue): PolicyEvent = {
      val fields = json.asJsObject.fields
      PolicyEvent(
        resourceUrn = fields.get("resourceUrn").map(_.convertTo[String]),
        message = fields.get("message").map(_.convertTo[String]).getOrElse(""),
        color = fields.get("color").map(_.convertTo[String]).getOrElse(""),
        policyName = fields.get("policyName").map(_.convertTo[String]).getOrElse(""),
        policyPackName = fields.get("policyPackName").map(_.convertTo[String]).getOrElse(""),
        policyPackVersion = fields.get("policyPackVersion").map(_.convertTo[String]).getOrElse(""),
        policyPackVersionTag = fields.get("policyPackVersionTag").map(_.convertTo[String]).getOrElse(""),
        enforcementLevel = fields.get("enforcementLevel").map(_.convertTo[String]).getOrElse(""),
        severity = fields.get("severity").map(_.convertTo[String]).getOrElse("")
      )
    }
  }
end PolicyEvent

/** Emitted during policy remediation. */
case class PolicyRemediationEvent(
  resourceUrn: Option[String],
  color: String,
  policyName: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  before: Option[Map[String, JsValue]],
  after: Option[Map[String, JsValue]]
)
object PolicyRemediationEvent:
  implicit object PolicyRemediationEventFormat extends RootJsonFormat[PolicyRemediationEvent] {
    def write(obj: PolicyRemediationEvent): JsValue = ???
    def read(json: JsValue): PolicyRemediationEvent = {
      val fields = json.asJsObject.fields
      PolicyRemediationEvent(
        resourceUrn = fields.get("resourceUrn").map(_.convertTo[String]),
        color = fields.get("color").map(_.convertTo[String]).getOrElse(""),
        policyName = fields.get("policyName").map(_.convertTo[String]).getOrElse(""),
        policyPackName = fields.get("policyPackName").map(_.convertTo[String]).getOrElse(""),
        policyPackVersion = fields.get("policyPackVersion").map(_.convertTo[String]).getOrElse(""),
        policyPackVersionTag = fields.get("policyPackVersionTag").map(_.convertTo[String]).getOrElse(""),
        before = fields.get("before").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[Map[String, JsValue]])
        },
        after = fields.get("after").flatMap {
          case JsNull => None
          case v      => Some(v.convertTo[Map[String, JsValue]])
        }
      )
    }
  }
end PolicyRemediationEvent

/** Emitted when a policy pack is loaded (empty event, signals load happened). */
case class PolicyLoadEvent()
object PolicyLoadEvent:
  implicit object PolicyLoadEventFormat extends RootJsonFormat[PolicyLoadEvent] {
    def write(obj: PolicyLoadEvent): JsValue = ???
    def read(json: JsValue): PolicyLoadEvent = PolicyLoadEvent()
  }
end PolicyLoadEvent

/** Summary of policy analysis for a resource. */
case class PolicyAnalyzeSummaryEvent(
  resourceUrn: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  passed: List[String],
  failed: List[String]
)
object PolicyAnalyzeSummaryEvent:
  implicit object PolicyAnalyzeSummaryEventFormat extends RootJsonFormat[PolicyAnalyzeSummaryEvent] {
    def write(obj: PolicyAnalyzeSummaryEvent): JsValue = ???
    def read(json: JsValue): PolicyAnalyzeSummaryEvent = {
      val fields = json.asJsObject.fields
      PolicyAnalyzeSummaryEvent(
        resourceUrn = fields.get("resourceUrn").map(_.convertTo[String]).getOrElse(""),
        policyPackName = fields.get("policyPackName").map(_.convertTo[String]).getOrElse(""),
        policyPackVersion = fields.get("policyPackVersion").map(_.convertTo[String]).getOrElse(""),
        policyPackVersionTag = fields.get("policyPackVersionTag").map(_.convertTo[String]).getOrElse(""),
        passed = fields.get("passed").map(_.convertTo[List[String]]).getOrElse(Nil),
        failed = fields.get("failed").map(_.convertTo[List[String]]).getOrElse(Nil)
      )
    }
  }
end PolicyAnalyzeSummaryEvent

/** Summary of policy remediation for a resource. */
case class PolicyRemediateSummaryEvent(
  resourceUrn: String,
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  passed: List[String],
  failed: List[String]
)
object PolicyRemediateSummaryEvent:
  implicit object PolicyRemediateSummaryEventFormat extends RootJsonFormat[PolicyRemediateSummaryEvent] {
    def write(obj: PolicyRemediateSummaryEvent): JsValue = ???
    def read(json: JsValue): PolicyRemediateSummaryEvent = {
      val fields = json.asJsObject.fields
      PolicyRemediateSummaryEvent(
        resourceUrn = fields.get("resourceUrn").map(_.convertTo[String]).getOrElse(""),
        policyPackName = fields.get("policyPackName").map(_.convertTo[String]).getOrElse(""),
        policyPackVersion = fields.get("policyPackVersion").map(_.convertTo[String]).getOrElse(""),
        policyPackVersionTag = fields.get("policyPackVersionTag").map(_.convertTo[String]).getOrElse(""),
        passed = fields.get("passed").map(_.convertTo[List[String]]).getOrElse(Nil),
        failed = fields.get("failed").map(_.convertTo[List[String]]).getOrElse(Nil)
      )
    }
  }
end PolicyRemediateSummaryEvent

/** Stack-level summary of policy analysis. */
case class PolicyAnalyzeStackSummaryEvent(
  policyPackName: String,
  policyPackVersion: String,
  policyPackVersionTag: String,
  passed: List[String],
  failed: List[String]
)
object PolicyAnalyzeStackSummaryEvent:
  implicit object PolicyAnalyzeStackSummaryEventFormat extends RootJsonFormat[PolicyAnalyzeStackSummaryEvent] {
    def write(obj: PolicyAnalyzeStackSummaryEvent): JsValue = ???
    def read(json: JsValue): PolicyAnalyzeStackSummaryEvent = {
      val fields = json.asJsObject.fields
      PolicyAnalyzeStackSummaryEvent(
        policyPackName = fields.get("policyPackName").map(_.convertTo[String]).getOrElse(""),
        policyPackVersion = fields.get("policyPackVersion").map(_.convertTo[String]).getOrElse(""),
        policyPackVersionTag = fields.get("policyPackVersionTag").map(_.convertTo[String]).getOrElse(""),
        passed = fields.get("passed").map(_.convertTo[List[String]]).getOrElse(Nil),
        failed = fields.get("failed").map(_.convertTo[List[String]]).getOrElse(Nil)
      )
    }
  }
end PolicyAnalyzeStackSummaryEvent

/** Emitted when a debugging session starts. */
case class StartDebuggingEvent(
  config: Map[String, JsValue]
)
object StartDebuggingEvent:
  implicit object StartDebuggingEventFormat extends RootJsonFormat[StartDebuggingEvent] {
    def write(obj: StartDebuggingEvent): JsValue = ???
    def read(json: JsValue): StartDebuggingEvent = {
      val fields = json.asJsObject.fields
      StartDebuggingEvent(
        config = fields.get("config").map(_.convertTo[Map[String, JsValue]]).getOrElse(Map.empty)
      )
    }
  }
end StartDebuggingEvent

/** Emitted for plugin download/install progress.
  *
  * Note: `completed` maps to JSON field `"received"` per Pulumi's Go source.
  */
case class ProgressEvent(
  `type`: ProgressType,
  id: String,
  message: String,
  completed: Long,
  total: Long,
  done: Boolean
)
object ProgressEvent:
  implicit object ProgressEventFormat extends RootJsonFormat[ProgressEvent] {
    def write(obj: ProgressEvent): JsValue = ???
    def read(json: JsValue): ProgressEvent = {
      val fields = json.asJsObject.fields
      ProgressEvent(
        `type` = fields.get("type").map(_.convertTo[ProgressType]).getOrElse(ProgressType.PluginDownload),
        id = fields.get("id").map(_.convertTo[String]).getOrElse(""),
        message = fields.get("message").map(_.convertTo[String]).getOrElse(""),
        completed = fields.get("received").map(_.convertTo[Long]).getOrElse(0L),
        total = fields.get("total").map(_.convertTo[Long]).getOrElse(0L),
        done = fields.get("done").exists(_.convertTo[Boolean])
      )
    }
  }
end ProgressEvent

/** Emitted when an internal engine error occurs. */
case class ErrorEvent(
  error: String
)
object ErrorEvent:
  implicit object ErrorEventFormat extends RootJsonFormat[ErrorEvent] {
    def write(obj: ErrorEvent): JsValue = ???
    def read(json: JsValue): ErrorEvent = {
      val fields = json.asJsObject.fields
      ErrorEvent(
        error = fields.get("error").map(_.convertTo[String]).getOrElse("")
      )
    }
  }
end ErrorEvent
