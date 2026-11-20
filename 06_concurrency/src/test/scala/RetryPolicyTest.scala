package dev.coilix.concurrency

import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class RetryPolicyTest extends FunSuite {
  private val exponential = RetryPolicy(5, 1.second, 5.seconds, 2.0, 0.0)

  test("doubles the wait after each failed try up to the limit") {
    assertEquals((1 to 4).toList.map(exponential.delayAfter), List(1.second, 2.seconds, 4.seconds, 5.seconds).map(Some(_)))
  }

  test("has no wait when no tries are left") {
    assertEquals((exponential.delayAfter(5), exponential.delayAfter(0)), (None, None))
    assertEquals((exponential.canRetry(4), exponential.canRetry(5)), (true, false))
  }

  test("spreads the wait by the jitter in both directions without exceeding the limit") {
    val spread = RetryPolicy(5, 1.second, 3.seconds, 2.0, 0.5)
    assertEquals(
      List(spread.delayAfter(1, 0.0), spread.delayAfter(1, 0.5), spread.delayAfter(1, 1.0), spread.delayAfter(3, 1.0)),
      List(500.millis, 1.second, 1500.millis, 3.seconds).map(Some(_))
    )
  }

  test("a random spread stays within the jitter") {
    val delays = List.fill(100)(RetryPolicy.default.delayAfter(1)).flatten
    assert(delays.forall(delay => delay >= 800.millis && delay <= 1200.millis), delays)
  }

  test("does not overflow after many tries") {
    assertEquals(RetryPolicy(1000, 1.second, 1.minute, 10.0, 0.0).delayAfter(999), Some(1.minute))
  }

  test("ready-made policies") {
    assertEquals(RetryPolicy.none.delayAfter(1), None)
    assertEquals((1 to 3).toList.map(RetryPolicy.fixed(3, 2.seconds).delayAfter), List(Some(2.seconds), Some(2.seconds), None))
    assertEquals(RetryPolicy.default.maxAttempts, 5)
  }

  test("rejects parameters out of range") {
    val invalid = List(
      () => RetryPolicy(0, 1.second, 1.second, 1.0, 0.0),
      () => RetryPolicy(1, (-1).second, 1.second, 1.0, 0.0),
      () => RetryPolicy(1, 2.seconds, 1.second, 1.0, 0.0),
      () => RetryPolicy(1, 1.second, 1.second, 0.5, 0.0),
      () => RetryPolicy(1, 1.second, 1.second, 1.0, 1.5)
    )
    assert(invalid.forall(create => scala.util.Try(create()).isFailure))
  }
}
