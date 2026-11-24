package dev.coilix.processing

import java.time.Instant
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class RestartPolicyTest extends FunSuite {

  private val now    = Instant.parse("2026-11-24T10:00:00Z")
  private val policy =
    RestartPolicy(Backoff.exponential(1.second, 10.seconds), maxAttempts = 3, within = 10.minutes, resetAfter = 5.minutes)

  private def minutesAgo(minutes: Long): Instant = now.minusSeconds(minutes * 60)

  test("restarts with a growing delay while the attempts last") {
    assertEquals(policy.afterFailure(Nil, minutesAgo(1), now, 0.5)._2, RestartDecision.Restart(1.second))
    assertEquals(policy.afterFailure(List(minutesAgo(1)), minutesAgo(1), now, 0.5)._2, RestartDecision.Restart(2.seconds))
  }

  test("gives up after more failures than maxAttempts within the window") {
    val failures = List(minutesAgo(1), minutesAgo(2), minutesAgo(3))
    assertEquals(policy.afterFailure(failures, minutesAgo(1), now, 0.5)._2, RestartDecision.GiveUp)
  }

  test("forgets failures older than the window") {
    val (counted, decision) = policy.afterFailure(List(minutesAgo(2), minutesAgo(11), minutesAgo(12)), minutesAgo(1), now, 0.5)
    assertEquals((counted, decision), (List(now, minutesAgo(2)), RestartDecision.Restart(2.seconds)))
  }

  test("starts counting again after a stable run of resetAfter") {
    val failures = List(minutesAgo(7), minutesAgo(8), minutesAgo(9))
    assertEquals(policy.afterFailure(failures, minutesAgo(6), now, 0.5), (List(now), RestartDecision.Restart(1.second)))
  }

  test("spreads delays by the jitter of the backoff") {
    val backoff = Backoff(1.second, 1.minute, 2.0, 0.5)
    assertEquals((backoff.delay(1, 0.0), backoff.delay(1, 1.0)), (500.millis, 1500.millis))
    assertEquals(backoff.delay(10, 1.0), 1.minute)
  }
}
