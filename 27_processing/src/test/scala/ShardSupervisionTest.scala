package dev.coilix.processing

import java.time.Instant
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ShardSupervisionTest extends FunSuite {

  private val start   = Instant.parse("2026-11-24T10:00:00Z")
  private val policy  = SupervisionPolicy(RestartPolicy(Backoff.fixed(1.second), maxAttempts = 2))
  private val failure = FailureInfo(FailureStage.Process, "java.lang.IllegalStateException", "offline", "", start)

  private def after(supervision: ShardSupervision, events: WorkerEvent*): SupervisionStep =
    events.foldLeft(SupervisionStep(supervision, Nil))((step, event) => step.supervision.on(event, start, policy, 0.5))

  private def running: ShardSupervision =
    after(ShardSupervision.idle(start), WorkerEvent.StartRequested, WorkerEvent.Prepared).supervision

  test("acquires and starts an idle shard") {
    val step = after(ShardSupervision.idle(start), WorkerEvent.StartRequested)
    assertEquals(
      (step.supervision.state, step.commands),
      (WorkerState.Preparing, List(WorkerCommand.Acquire, WorkerCommand.StartWorker))
    )
  }

  test("ignores a start of a running shard") {
    assertEquals(after(running, WorkerEvent.StartRequested).commands, Nil)
  }

  test("waits for the backoff after a failure and starts again when it is due") {
    val failed = after(running, WorkerEvent.Crashed(failure))
    assertEquals(failed.supervision.state, WorkerState.Backoff)
    assertEquals(failed.commands, List(WorkerCommand.ScheduleRestart(1.second)))
    assertEquals(failed.supervision.lastFailure, Some(failure))
    val due    = after(failed.supervision, WorkerEvent.RestartDue)
    assertEquals((due.supervision.state, due.commands), (WorkerState.Preparing, List(WorkerCommand.StartWorker)))
  }

  test("gives up after more failures than the policy allows and releases the shard") {
    val step = after(
      running,
      WorkerEvent.Crashed(failure),
      WorkerEvent.RestartDue,
      WorkerEvent.Crashed(failure),
      WorkerEvent.RestartDue,
      WorkerEvent.Crashed(failure)
    )
    assertEquals(step.supervision.state, WorkerState.Failed)
    assertEquals((step.supervision.restarts, step.commands), (2, List(WorkerCommand.Release(ReleaseReason.Failed))))
  }

  test("starts a failed shard again with fresh attempts") {
    val failed = after(running, WorkerEvent.Crashed(failure), WorkerEvent.RestartDue, WorkerEvent.Crashed(failure))
    val step   = after(failed.supervision.copy(state = WorkerState.Failed), WorkerEvent.StartRequested)
    assertEquals((step.supervision.state, step.supervision.failures), (WorkerState.Preparing, Nil))
  }

  test("completes or restarts after the work ends, as the completion policy says") {
    assertEquals(after(running, WorkerEvent.Finished).commands, List(WorkerCommand.Release(ReleaseReason.Completed)))
    val restarting = running.on(WorkerEvent.Finished, start, policy.copy(completion = CompletionPolicy.Restart(3.seconds)), 0.5)
    assertEquals(
      (restarting.supervision.state, restarting.commands),
      (WorkerState.Backoff, List(WorkerCommand.ScheduleRestart(3.seconds)))
    )
  }

  test("stops a running shard and releases it with the reason once the worker stopped") {
    val stopping = after(running, WorkerEvent.StopRequested(ReleaseReason.Shutdown))
    assertEquals((stopping.supervision.state, stopping.commands), (WorkerState.Stopping, List(WorkerCommand.StopWorker)))
    val stopped  = after(stopping.supervision, WorkerEvent.WorkerStopped)
    assertEquals(
      (stopped.supervision.state, stopped.commands),
      (WorkerState.Stopped, List(WorkerCommand.Release(ReleaseReason.Shutdown)))
    )
  }

  test("cancels a scheduled restart when a waiting shard stops") {
    val waiting = after(running, WorkerEvent.Crashed(failure)).supervision
    assertEquals(
      after(waiting, WorkerEvent.StopRequested(ReleaseReason.Stopped)).commands,
      List(WorkerCommand.CancelRestart, WorkerCommand.Release(ReleaseReason.Stopped))
    )
  }

  test("ignores late reports of a stopped worker and stops of a stopped shard") {
    val stopped = after(running, WorkerEvent.StopRequested(ReleaseReason.Stopped), WorkerEvent.WorkerStopped).supervision
    assertEquals(after(stopped, WorkerEvent.Finished).commands, Nil)
    assertEquals(after(stopped, WorkerEvent.Crashed(failure)).commands, Nil)
    assertEquals(after(stopped, WorkerEvent.RestartDue).commands, Nil)
    assertEquals(after(stopped, WorkerEvent.StopRequested(ReleaseReason.Shutdown)).commands, Nil)
  }

  test("restarts a running shard by stopping its worker and starting a new one, without releasing it") {
    assertEquals(after(running, WorkerEvent.RestartRequested).commands, List(WorkerCommand.StopWorker, WorkerCommand.StartWorker))
    val waiting = after(running, WorkerEvent.Crashed(failure)).supervision
    assertEquals(
      after(waiting, WorkerEvent.RestartRequested).commands,
      List(WorkerCommand.CancelRestart, WorkerCommand.StartWorker)
    )
    val idle    = after(ShardSupervision.idle(start), WorkerEvent.RestartRequested)
    assertEquals(idle.commands, List(WorkerCommand.Acquire, WorkerCommand.StartWorker))
  }

  test("ends a stopping shard when its worker reports the end itself") {
    val stopping = after(running, WorkerEvent.StopRequested(ReleaseReason.Stopped)).supervision
    assertEquals(after(stopping, WorkerEvent.Finished).supervision.state, WorkerState.Stopped)
    assertEquals(after(stopping, WorkerEvent.RestartRequested).commands, Nil)
  }
}
