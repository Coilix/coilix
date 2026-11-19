package dev.coilix.concurrency

import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ScheduledTriggerTest extends FunSuite {

  test("describes a trigger without an initial delay") {
    assertEquals(ScheduledTrigger.FixedRate(0.seconds, 1.second).toString, "at a fixed rate of 1 second")
  }

  test("describes a trigger with an initial delay") {
    assertEquals(ScheduledTrigger.FixedDelay(10.seconds, 1.minute).toString, "with a fixed delay of 1 minute after 10 seconds")
  }
}
