package dev.coilix.diagnostics

import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ThreadChecksTest extends FunSuite {
  private val worker  = Samples.thread(1, "worker", Thread.State.RUNNABLE)
  private val blocked = Samples.thread(2, "blocked", Thread.State.BLOCKED)

  test("the history keeps the time a thread entered its state and forgets threads that are gone") {
    val first  = ThreadHistory.empty.next(Samples.at(0, List(worker, blocked), 2), 1.minute)
    val second = first.next(Samples.at(10, List(blocked), 1), 1.minute)
    assertEquals(second.states, Map(2L -> StateSince(Thread.State.BLOCKED, Samples.start)))
    assertEquals(second.firstSample, Some(Samples.start))
  }

  test("the history restarts the time when the state changes and drops counts outside the window") {
    val first  = ThreadHistory.empty.next(Samples.at(0, List(blocked), 1), 30.seconds)
    val second = first.next(Samples.at(40, List(blocked.copy(state = Thread.State.RUNNABLE)), 3), 30.seconds)
    assertEquals(second.states(2L), StateSince(Thread.State.RUNNABLE, Samples.start.plusSeconds(40)))
    assertEquals(second.counts.map(_.count), List(3))
  }

  test("DeadlockCheck reports deadlocked threads as Critical") {
    val sample   = Samples.at(0, List(worker, blocked), 2, deadlocked = Set(1L, 2L))
    val findings = DeadlockCheck().check(sample, ThreadHistory.empty)
    assertEquals(findings.map(f => (f.severity, f.kind, f.threads.size)), List((FindingSeverity.Critical, Finding.deadlock, 2)))
    assertEquals(findings.head.message, "2 threads are deadlocked: blocked, worker")
    assertEquals(DeadlockCheck().check(Samples.at(0, List(worker), 1), ThreadHistory.empty), Nil)
  }

  test("ThreadCountCheck warns from the warning count and is Critical from the critical count") {
    val check = ThreadCountCheck(100, 200)
    assertEquals(
      List(99, 100, 200).map(count => check.check(Samples.at(0, Nil, count), ThreadHistory.empty).map(_.severity)),
      List(Nil, List(FindingSeverity.Warning), List(FindingSeverity.Critical))
    )
  }

  test("LongBlockedCheck warns about a thread blocked for at least the threshold") {
    val check   = LongBlockedCheck(30.seconds)
    val early   = ThreadHistory.empty.next(Samples.at(0, List(worker, blocked), 2), 5.minutes)
    val later   = Samples.at(30, List(worker, blocked), 2)
    val history = early.next(later, 5.minutes)
    assertEquals(check.check(Samples.at(0, List(worker, blocked), 2), early), Nil)
    assertEquals(check.check(later, history).flatMap(_.threads.map(_.name)), List("blocked"))
  }

  test("ThreadGrowthCheck warns about a growth by the limit after the grace period") {
    val check   = ThreadGrowthCheck(50, 1.minute)
    val history = List(Samples.at(0, Nil, 20), Samples.at(30, Nil, 40), Samples.at(70, Nil, 80))
      .scanLeft(ThreadHistory.empty)((seen, sample) => seen.next(sample, 5.minutes))
    assertEquals(check.check(Samples.at(30, Nil, 40), history(2)), Nil)
    assertEquals(check.check(Samples.at(70, Nil, 80), history(3)).map(_.kind), List(Finding.threadGrowth))
  }
}
