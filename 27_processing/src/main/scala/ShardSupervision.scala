package dev.coilix.processing

import java.time.Instant

/**
 * The supervision of one shard as pure data: its state and the failures that count for the restart policy.
 *
 * [[dev.coilix.processing.ShardSupervision.on]] turns an event into the next supervision and the commands the manager
 * runs, so engines only run workers and report events, and the supervision rules are the same for every engine. Events
 * that do not fit the state, such as a late report of a worker that was already stopped, change nothing.
 *
 * @param state
 *   the state of the worker
 * @param since
 *   when the worker entered the state
 * @param failures
 *   the failures that count for the restart policy, newest first
 * @param restarts
 *   how many times the worker was restarted after a failure
 * @param lastFailure
 *   the latest failure, kept after the worker recovers
 * @param stopReason
 *   why the worker is stopping, while it is `Stopping`
 */
final case class ShardSupervision(
  state:       WorkerState,
  since:       Instant,
  failures:    List[Instant]         = Nil,
  restarts:    Int                   = 0,
  lastFailure: Option[FailureInfo]   = None,
  stopReason:  Option[ReleaseReason] = None
) {

  /**
   * The supervision after the event at `now`.
   *
   * @param random
   *   a number from 0 to 1 for the jitter of restart delays
   */
  def on(event: WorkerEvent, now: Instant, policy: SupervisionPolicy, random: Double): SupervisionStep =
    event match {
      case WorkerEvent.StartRequested        => if (state.isActive) unchanged else acquired(now)
      case WorkerEvent.RestartRequested      => restarted(now)
      case WorkerEvent.Prepared              => if (state == WorkerState.Preparing) moved(WorkerState.Running, now) else unchanged
      case WorkerEvent.Finished              => ended(now)(completed(now, policy.completion))
      case WorkerEvent.Crashed(failure)      => ended(now)(crashed(failure, now, policy.restart, random))
      case WorkerEvent.RestartDue            => if (state == WorkerState.Backoff) started(now) else unchanged
      case WorkerEvent.StopRequested(reason) => stopping(reason, now)
      case WorkerEvent.WorkerStopped         => if (state == WorkerState.Stopping) stopped(now) else unchanged
    }

  private def unchanged: SupervisionStep = SupervisionStep(this, Nil)

  private def moved(next: WorkerState, now: Instant, commands: WorkerCommand*): SupervisionStep =
    SupervisionStep(copy(state = next, since = now), commands.toList)

  private def acquired(now: Instant): SupervisionStep =
    SupervisionStep(
      copy(state = WorkerState.Preparing, since = now, failures = Nil),
      List(WorkerCommand.Acquire, WorkerCommand.StartWorker)
    )

  private def started(now: Instant): SupervisionStep = moved(WorkerState.Preparing, now, WorkerCommand.StartWorker)

  private def restarted(now: Instant): SupervisionStep =
    state match {
      case WorkerState.Preparing | WorkerState.Running =>
        moved(WorkerState.Preparing, now, WorkerCommand.StopWorker, WorkerCommand.StartWorker)
      case WorkerState.Backoff                         =>
        moved(WorkerState.Preparing, now, WorkerCommand.CancelRestart, WorkerCommand.StartWorker)
      case WorkerState.Stopping                        => unchanged
      case _                                           => acquired(now)
    }

  private def ended(now: Instant)(whenWorking: => SupervisionStep): SupervisionStep =
    state match {
      case WorkerState.Preparing | WorkerState.Running => whenWorking
      case WorkerState.Stopping                        => stopped(now)
      case _                                           => unchanged
    }

  private def completed(now: Instant, completion: CompletionPolicy): SupervisionStep =
    completion match {
      case CompletionPolicy.Stop           => moved(WorkerState.Completed, now, WorkerCommand.Release(ReleaseReason.Completed))
      case CompletionPolicy.Restart(delay) => moved(WorkerState.Backoff, now, WorkerCommand.ScheduleRestart(delay))
    }

  private def crashed(failure: FailureInfo, now: Instant, policy: RestartPolicy, random: Double): SupervisionStep = {
    val (counted, decision) = policy.afterFailure(failures, since, now, random)
    val failed              = copy(since = now, failures = counted, lastFailure = Option(failure))
    decision match {
      case RestartDecision.Restart(delay) =>
        SupervisionStep(
          failed.copy(state = WorkerState.Backoff, restarts = restarts + 1),
          List(WorkerCommand.ScheduleRestart(delay))
        )
      case RestartDecision.GiveUp         =>
        SupervisionStep(failed.copy(state = WorkerState.Failed), List(WorkerCommand.Release(ReleaseReason.Failed)))
    }
  }

  private def stopping(reason: ReleaseReason, now: Instant): SupervisionStep =
    state match {
      case WorkerState.Preparing | WorkerState.Running =>
        SupervisionStep(
          copy(state = WorkerState.Stopping, since = now, stopReason = Option(reason)),
          List(WorkerCommand.StopWorker)
        )
      case WorkerState.Backoff                         =>
        moved(WorkerState.Stopped, now, WorkerCommand.CancelRestart, WorkerCommand.Release(reason))
      case _                                           => unchanged
    }

  private def stopped(now: Instant): SupervisionStep =
    SupervisionStep(
      copy(state = WorkerState.Stopped, since = now, stopReason = None),
      List(WorkerCommand.Release(stopReason.getOrElse(ReleaseReason.Stopped)))
    )
}

/** Creates supervisions. */
object ShardSupervision {

  /** A shard assigned to this replica at `now` that was never started. */
  def idle(now: Instant): ShardSupervision = ShardSupervision(WorkerState.Idle, now)
}
