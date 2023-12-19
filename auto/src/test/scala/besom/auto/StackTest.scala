package besom.auto

import besom.util.eitherOps

//noinspection ScalaFileName
class EngineEventJSONTest extends munit.FunSuite {

  test("EngineEvent should deserialize from JSON") {
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
}
