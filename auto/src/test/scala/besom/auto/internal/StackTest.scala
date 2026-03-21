package besom.auto.internal

import besom.json.*
import besom.util.eitherOps

//noinspection ScalaFileName
class EngineEventJSONTest extends munit.FunSuite {

  // ── SummaryEvent (existing, backward compat) ────────────────────────

  test("EngineEvent should deserialize SummaryEvent from JSON") {
    val json =
      """{"sequence":4,"timestamp":1704893590,"summaryEvent":{"maybeCorrupt":false,"durationSeconds":0,"resourceChanges":{"create":1},"PolicyPacks":{}}}"""

    val event: EngineEvent = EngineEvent.fromJson(json).get

    assertEquals(event.sequence, 4)
    assertEquals(event.timestamp, 1704893590)
    assert(event.summaryEvent.isDefined)
    val summary: SummaryEvent = event.summaryEvent.get
    assertEquals(summary.maybeCorrupt, false)
    assertEquals(summary.durationSeconds, 0)
    assertEquals(summary.resourceChanges(OpType.Create), 1)
    assertEquals(summary.policyPacks, Map.empty)
  }

  // ── CancelEvent ─────────────────────────────────────────────────────

  test("CancelEvent should deserialize from JSON") {
    val json  = """{"sequence":99,"timestamp":1704893600,"cancelEvent":{}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.cancelEvent.isDefined)
    assertEquals(event.cancelEvent.get, CancelEvent())
    // all others should be None
    assert(event.summaryEvent.isEmpty)
    assert(event.diagnosticEvent.isEmpty)
  }

  // ── StdoutEngineEvent ───────────────────────────────────────────────

  test("StdoutEngineEvent should deserialize from JSON") {
    val json =
      """{"sequence":2,"timestamp":1704893591,"stdoutEvent":{"message":"Previewing update (dev)","color":"auto"}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.stdoutEvent.isDefined)
    val stdout = event.stdoutEvent.get
    assertEquals(stdout.message, "Previewing update (dev)")
    assertEquals(stdout.color, "auto")
  }

  // ── PreludeEvent ────────────────────────────────────────────────────

  test("PreludeEvent should deserialize from JSON") {
    val json =
      """{"sequence":1,"timestamp":1704893590,"preludeEvent":{"config":{"aws:region":"us-east-1"}}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.preludeEvent.isDefined)
    assertEquals(event.preludeEvent.get.config, Map("aws:region" -> "us-east-1"))
  }

  test("PreludeEvent with empty config") {
    val json  = """{"sequence":1,"timestamp":1704893590,"preludeEvent":{"config":{}}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.preludeEvent.isDefined)
    assertEquals(event.preludeEvent.get.config, Map.empty[String, String])
  }

  // ── DiagnosticEvent ─────────────────────────────────────────────────

  test("DiagnosticEvent should deserialize from JSON") {
    val json =
      """{"sequence":1,"timestamp":1704893590,"diagnosticEvent":{"severity":"info","message":"Updating (dev)","color":"auto","streamID":0,"ephemeral":false}}"""

    val event = EngineEvent.fromJson(json).get

    assert(event.diagnosticEvent.isDefined)
    val diag = event.diagnosticEvent.get
    assertEquals(diag.severity, "info")
    assertEquals(diag.message, "Updating (dev)")
    assertEquals(diag.color, "auto")
    assertEquals(diag.streamID, Some(0))
    assertEquals(diag.ephemeral, false)
    assertEquals(diag.urn, None)
    assertEquals(diag.prefix, None)
  }

  test("DiagnosticEvent with urn and prefix") {
    val json =
      """{"sequence":2,"timestamp":1704893591,"diagnosticEvent":{"urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","prefix":"aws:s3/bucket:Bucket (my-bucket): ","severity":"warning","message":"deprecated field","color":"","ephemeral":true}}"""

    val event = EngineEvent.fromJson(json).get
    val diag  = event.diagnosticEvent.get
    assertEquals(diag.urn, Some("urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket"))
    assertEquals(diag.prefix, Some("aws:s3/bucket:Bucket (my-bucket): "))
    assertEquals(diag.severity, "warning")
    assertEquals(diag.ephemeral, true)
  }

  // ── ResourcePreEvent ────────────────────────────────────────────────

  test("ResourcePreEvent should deserialize from JSON") {
    val json =
      """{"sequence":3,"timestamp":1704893591,"resourcePreEvent":{"metadata":{"op":"create","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","type":"aws:s3/bucket:Bucket","provider":"urn:pulumi:dev::myproject::pulumi:providers:aws::default_6_0_0::id","new":{"type":"aws:s3/bucket:Bucket","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","custom":true,"id":"","parent":"urn:pulumi:dev::myproject::pulumi:pulumi:Stack::myproject-dev","inputs":{"bucket":"my-bucket"},"outputs":{},"provider":"urn:pulumi:dev::myproject::pulumi:providers:aws::default_6_0_0::id"},"logical":false},"planning":true}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.resourcePreEvent.isDefined)
    val pre = event.resourcePreEvent.get
    assertEquals(pre.planning, true)
    assertEquals(pre.metadata.op, OpType.Create)
    assertEquals(pre.metadata.urn, "urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket")
    assertEquals(pre.metadata.`type`, "aws:s3/bucket:Bucket")
    assert(pre.metadata.old.isEmpty)
    assert(pre.metadata.`new`.isDefined)
    val newState = pre.metadata.`new`.get
    assertEquals(newState.`type`, "aws:s3/bucket:Bucket")
    assertEquals(newState.custom, true)
    assert(newState.inputs.isDefined)
    assertEquals(newState.inputs.get("bucket"), JsString("my-bucket"))
  }

  test("ResourcePreEvent with detailed diff") {
    val json =
      """{"sequence":5,"timestamp":1704893592,"resourcePreEvent":{"metadata":{"op":"update","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","type":"aws:s3/bucket:Bucket","provider":"","old":{"type":"aws:s3/bucket:Bucket","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","custom":true,"id":"bucket-123","parent":"","inputs":{"bucket":"my-bucket"},"outputs":{"arn":"arn:aws:s3:::my-bucket"},"provider":""},"new":{"type":"aws:s3/bucket:Bucket","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","custom":true,"id":"","parent":"","inputs":{"bucket":"my-bucket","tags":{"env":"dev"}},"outputs":null,"provider":""},"diffs":["tags"],"detailedDiff":{"tags":{"diffKind":"add","inputDiff":true}},"logical":false},"planning":false}}"""

    val event = EngineEvent.fromJson(json).get
    val pre   = event.resourcePreEvent.get
    assertEquals(pre.metadata.op, OpType.Update)
    assert(pre.metadata.old.isDefined)
    assertEquals(pre.metadata.old.get.id, "bucket-123")
    assert(pre.metadata.diffs.isDefined)
    assertEquals(pre.metadata.diffs.get, List("tags"))
    assert(pre.metadata.detailedDiff.isDefined)
    val tagDiff = pre.metadata.detailedDiff.get("tags")
    assertEquals(tagDiff.diffKind, DiffKind.Add)
    assertEquals(tagDiff.inputDiff, true)
  }

  // ── ResOutputsEvent ─────────────────────────────────────────────────

  test("ResOutputsEvent should deserialize from JSON") {
    val json =
      """{"sequence":6,"timestamp":1704893593,"resOutputsEvent":{"metadata":{"op":"create","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","type":"aws:s3/bucket:Bucket","provider":"","new":{"type":"aws:s3/bucket:Bucket","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","custom":true,"id":"bucket-123","parent":"","inputs":null,"outputs":{"arn":"arn:aws:s3:::my-bucket","bucket":"my-bucket"},"provider":""},"logical":false},"planning":false}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.resOutputsEvent.isDefined)
    val outputs = event.resOutputsEvent.get
    assertEquals(outputs.planning, false)
    assertEquals(outputs.metadata.op, OpType.Create)
    assert(outputs.metadata.`new`.isDefined)
    assertEquals(outputs.metadata.`new`.get.id, "bucket-123")
    assert(outputs.metadata.`new`.get.outputs.isDefined)
  }

  // ── ResOpFailedEvent ────────────────────────────────────────────────

  test("ResOpFailedEvent should deserialize from JSON") {
    val json =
      """{"sequence":7,"timestamp":1704893594,"resOpFailedEvent":{"metadata":{"op":"create","urn":"urn:pulumi:dev::myproject::aws:s3/bucket:Bucket::my-bucket","type":"aws:s3/bucket:Bucket","provider":""},"status":2,"steps":1}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.resOpFailedEvent.isDefined)
    val failed = event.resOpFailedEvent.get
    assertEquals(failed.metadata.op, OpType.Create)
    assertEquals(failed.status, 2)
    assertEquals(failed.steps, 1)
  }

  // ── PolicyEvent ─────────────────────────────────────────────────────

  test("PolicyEvent should deserialize from JSON") {
    val json =
      """{"sequence":10,"timestamp":1704893595,"policyEvent":{"resourceUrn":"urn:pulumi:dev::proj::aws:s3/bucket:Bucket::b","message":"must have tags","color":"auto","policyName":"require-tags","policyPackName":"aws-best-practices","policyPackVersion":"1.0.0","policyPackVersionTag":"latest","enforcementLevel":"mandatory","severity":"high"}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.policyEvent.isDefined)
    val pe = event.policyEvent.get
    assertEquals(pe.resourceUrn, Some("urn:pulumi:dev::proj::aws:s3/bucket:Bucket::b"))
    assertEquals(pe.message, "must have tags")
    assertEquals(pe.policyName, "require-tags")
    assertEquals(pe.policyPackName, "aws-best-practices")
    assertEquals(pe.policyPackVersion, "1.0.0")
    assertEquals(pe.enforcementLevel, "mandatory")
    assertEquals(pe.severity, Some("high"))
  }

  // ── PolicyRemediationEvent ──────────────────────────────────────────

  test("PolicyRemediationEvent should deserialize from JSON") {
    val json =
      """{"sequence":11,"timestamp":1704893596,"policyRemediationEvent":{"resourceUrn":"urn:pulumi:dev::proj::aws:s3/bucket:Bucket::b","color":"auto","policyName":"add-tags","policyPackName":"aws-best-practices","policyPackVersion":"1.0.0","policyPackVersionTag":"latest","before":{"tags":null},"after":{"tags":{"managed":"true"}}}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.policyRemediationEvent.isDefined)
    val pr = event.policyRemediationEvent.get
    assertEquals(pr.policyName, "add-tags")
    assert(pr.after.isDefined)
    assertEquals(pr.after.get("tags"), JsObject("managed" -> JsString("true")))
  }

  // ── PolicyLoadEvent ─────────────────────────────────────────────────

  test("PolicyLoadEvent should deserialize from JSON") {
    val json  = """{"sequence":8,"timestamp":1704893595,"policyLoadEvent":{}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.policyLoadEvent.isDefined)
    assertEquals(event.policyLoadEvent.get, PolicyLoadEvent())
  }

  // ── PolicyAnalyzeSummaryEvent ───────────────────────────────────────

  test("PolicyAnalyzeSummaryEvent should deserialize from JSON") {
    val json =
      """{"sequence":12,"timestamp":1704893597,"policyAnalyzeSummaryEvent":{"resourceUrn":"urn:pulumi:dev::proj::aws:s3/bucket:Bucket::b","policyPackName":"aws-best-practices","policyPackVersion":"1.0.0","policyPackVersionTag":"latest","passed":["rule-a","rule-b"],"failed":["rule-c"]}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.policyAnalyzeSummaryEvent.isDefined)
    val pas = event.policyAnalyzeSummaryEvent.get
    assertEquals(pas.resourceUrn, "urn:pulumi:dev::proj::aws:s3/bucket:Bucket::b")
    assertEquals(pas.policyPackName, "aws-best-practices")
    assertEquals(pas.passed, List("rule-a", "rule-b"))
    assertEquals(pas.failed, List("rule-c"))
  }

  // ── PolicyRemediateSummaryEvent ─────────────────────────────────────

  test("PolicyRemediateSummaryEvent should deserialize from JSON") {
    val json =
      """{"sequence":13,"timestamp":1704893598,"policyRemediateSummaryEvent":{"resourceUrn":"urn:pulumi:dev::proj::aws:s3/bucket:Bucket::b","policyPackName":"aws-best-practices","policyPackVersion":"1.0.0","policyPackVersionTag":"latest","passed":["fix-a"],"failed":[]}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.policyRemediateSummaryEvent.isDefined)
    val prs = event.policyRemediateSummaryEvent.get
    assertEquals(prs.passed, List("fix-a"))
    assertEquals(prs.failed, Nil)
  }

  // ── PolicyAnalyzeStackSummaryEvent ──────────────────────────────────

  test("PolicyAnalyzeStackSummaryEvent should deserialize from JSON") {
    val json =
      """{"sequence":14,"timestamp":1704893599,"policyAnalyzeStackSummaryEvent":{"policyPackName":"aws-best-practices","policyPackVersion":"1.0.0","policyPackVersionTag":"latest","passed":["stack-rule"]}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.policyAnalyzeStackSummaryEvent.isDefined)
    val pass = event.policyAnalyzeStackSummaryEvent.get
    assertEquals(pass.policyPackName, "aws-best-practices")
    assertEquals(pass.passed, List("stack-rule"))
    assertEquals(pass.failed, Nil)
  }

  // ── StartDebuggingEvent ─────────────────────────────────────────────

  test("StartDebuggingEvent should deserialize from JSON") {
    val json =
      """{"sequence":15,"timestamp":1704893600,"startDebuggingEvent":{"config":{"port":"12345","host":"localhost"}}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.startDebuggingEvent.isDefined)
    assertEquals(event.startDebuggingEvent.get.config("port"), JsString("12345"))
    assertEquals(event.startDebuggingEvent.get.config("host"), JsString("localhost"))
  }

  test("StartDebuggingEvent with empty config") {
    val json  = """{"sequence":15,"timestamp":1704893600,"startDebuggingEvent":{}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.startDebuggingEvent.isDefined)
    assertEquals(event.startDebuggingEvent.get.config, Map.empty[String, JsValue])
  }

  // ── ProgressEvent ───────────────────────────────────────────────────

  test("ProgressEvent should deserialize from JSON") {
    val json =
      """{"sequence":16,"timestamp":1704893601,"progressEvent":{"type":"plugin-download","id":"pulumi-resource-aws-v6.0.0","message":"Downloading...","received":524288,"total":1048576,"done":false}}"""

    val event = EngineEvent.fromJson(json).get
    assert(event.progressEvent.isDefined)
    val pe = event.progressEvent.get
    assertEquals(pe.`type`, ProgressType.PluginDownload)
    assertEquals(pe.id, "pulumi-resource-aws-v6.0.0")
    assertEquals(pe.message, "Downloading...")
    assertEquals(pe.received, 524288L)
    assertEquals(pe.total, 1048576L)
    assertEquals(pe.done, false)
  }

  test("ProgressEvent with plugin-install type") {
    val json =
      """{"sequence":17,"timestamp":1704893602,"progressEvent":{"type":"plugin-install","id":"pulumi-resource-aws-v6.0.0","message":"Installing...","received":1048576,"total":1048576,"done":true}}"""
    val event = EngineEvent.fromJson(json).get
    val pe    = event.progressEvent.get
    assertEquals(pe.`type`, ProgressType.PluginInstall)
    assertEquals(pe.done, true)
    assertEquals(pe.received, 1048576L)
  }

  // ── ErrorEvent ──────────────────────────────────────────────────────

  test("ErrorEvent should deserialize from JSON") {
    val json =
      """{"sequence":18,"timestamp":1704893603,"errorEvent":{"error":"internal engine error: snapshot integrity failure"}}"""
    val event = EngineEvent.fromJson(json).get
    assert(event.errorEvent.isDefined)
    assertEquals(event.errorEvent.get.error, "internal engine error: snapshot integrity failure")
  }

  // ── Unknown event type ──────────────────────────────────────────────

  test("EngineEvent with unknown event type should have all None") {
    val json = """{"sequence":1,"timestamp":1704893590,"someNewFutureEvent":{"data":"stuff"}}"""

    val event = EngineEvent.fromJson(json).get
    assertEquals(event.sequence, 1)
    assert(event.cancelEvent.isEmpty)
    assert(event.stdoutEvent.isEmpty)
    assert(event.diagnosticEvent.isEmpty)
    assert(event.preludeEvent.isEmpty)
    assert(event.summaryEvent.isEmpty)
    assert(event.resourcePreEvent.isEmpty)
    assert(event.resOutputsEvent.isEmpty)
    assert(event.resOpFailedEvent.isEmpty)
    assert(event.policyEvent.isEmpty)
    assert(event.policyRemediationEvent.isEmpty)
    assert(event.policyLoadEvent.isEmpty)
    assert(event.policyAnalyzeSummaryEvent.isEmpty)
    assert(event.policyRemediateSummaryEvent.isEmpty)
    assert(event.policyAnalyzeStackSummaryEvent.isEmpty)
    assert(event.startDebuggingEvent.isEmpty)
    assert(event.progressEvent.isEmpty)
    assert(event.errorEvent.isEmpty)
  }

  // ── Supporting types ────────────────────────────────────────────────

  test("DiffKind should round-trip") {
    assertEquals(DiffKind.from("add"), DiffKind.Add)
    assertEquals(DiffKind.from("add-replace"), DiffKind.AddReplace)
    assertEquals(DiffKind.from("delete"), DiffKind.Delete)
    assertEquals(DiffKind.from("delete-replace"), DiffKind.DeleteReplace)
    assertEquals(DiffKind.from("update"), DiffKind.Update)
    assertEquals(DiffKind.from("update-replace"), DiffKind.UpdateReplace)

    assert(DiffKind.AddReplace.forcesReplacement)
    assert(DiffKind.DeleteReplace.forcesReplacement)
    assert(DiffKind.UpdateReplace.forcesReplacement)
    assert(!DiffKind.Add.forcesReplacement)
    assert(!DiffKind.Delete.forcesReplacement)
    assert(!DiffKind.Update.forcesReplacement)
  }

  test("StepEventStateMetadata defaults omitempty fields when absent") {
    val json =
      """{"type":"pulumi:pulumi:Stack","urn":"urn:pulumi:dev::proj::pulumi:pulumi:Stack::proj-dev","id":"","parent":"","inputs":{"key":"val"},"outputs":null,"provider":""}"""

    val state = json.parseJson[StepEventStateMetadata].getOrElse(fail("Failed to parse"))
    assertEquals(state.`type`, "pulumi:pulumi:Stack")
    assertEquals(state.urn, "urn:pulumi:dev::proj::pulumi:pulumi:Stack::proj-dev")
    // omitempty bools default to false when absent
    assertEquals(state.custom, false)
    assertEquals(state.delete, false)
    assertEquals(state.protect, false)
    assertEquals(state.retainOnDelete, false)
    // omitempty Option defaults to None when absent
    assertEquals(state.initErrors, None)
    // nullable maps
    assertEquals(state.inputs, Some(Map("key" -> JsString("val"))))
    assertEquals(state.outputs, None)
  }

  test("StepEventMetadata handles 'new' keyword field") {
    val json =
      """{"op":"create","urn":"urn:pulumi:dev::proj::pulumi:pulumi:Stack::proj-dev","type":"pulumi:pulumi:Stack","provider":"","old":null,"new":{"type":"pulumi:pulumi:Stack","urn":"urn:pulumi:dev::proj::pulumi:pulumi:Stack::proj-dev","id":"","parent":"","inputs":null,"outputs":null,"provider":""},"detailedDiff":null}"""

    val meta = json.parseJson[StepEventMetadata].getOrElse(fail("Failed to parse"))
    assertEquals(meta.op, OpType.Create)
    assert(meta.old.isEmpty)
    assert(meta.`new`.isDefined)
    assertEquals(meta.`new`.get.`type`, "pulumi:pulumi:Stack")
  }

  test("ProgressType round-trip") {
    assertEquals(ProgressType.from("plugin-download"), ProgressType.PluginDownload)
    assertEquals(ProgressType.from("plugin-install"), ProgressType.PluginInstall)
  }
}
