package dev.coilix.processing

import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import dev.coilix.cluster.ReplicaIdentity
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.TaskScheduler
import dev.coilix.concurrency.ThreadFactories
import dev.coilix.context.ApplicationContext
import dev.coilix.context.CollectedComponents
import dev.coilix.context.ContextLifecycleListener
import dev.coilix.events.EventPublisher
import java.time.Clock
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Deadline
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

/**
 * The [[dev.coilix.processing.ProcessorManager]] of the `processing` module, created as the `processorManager` bean.
 *
 * It creates the processors when it is created, starts the enabled ones when the context has started and
 * `coilix.processors.autostart` is true, and stops every shard in the first phase of closing, within
 * `coilix.processors.shutdown-timeout`, logging the shards that did not stop in time. The state of the shards is an
 * immutable map replaced by the control thread, so reading it never waits. Every change of the state of a shard and every
 * failure of a worker is published as a [[dev.coilix.processing.ProcessorEvent]] when an `EventPublisher` is given.
 */
@Slf4s
final class DefaultProcessorManager(
  collected:   CollectedComponents,
  engines:     List[ProcessorEngine],
  identity:    ReplicaIdentity,
  settings:    ProcessingSettings,
  scheduler:   TaskScheduler,
  fatalErrors: FatalErrorHandler,
  publisher:   Option[EventPublisher],
  clock:       Clock
) extends ProcessorManager
    with ContextLifecycleListener {

  private val managed: List[ManagedProcessor] = ProcessorAssembler(collected, engines, identity, settings).assemble()

  private val byId: Map[ProcessorId, ManagedProcessor] = managed.map(processor => processor.id -> processor).toMap

  private val runs: AtomicReference[Map[ShardKey, ShardRun]] = AtomicReference(initialRuns)

  private val control = ControlThread("coilix-processor-manager", fatalErrors)

  private val stopThreads = ThreadFactories.named("coilix-processor-stop-", virtual = true)

  private val stopContext = ExecutionContext.fromExecutor(task => stopThreads.newThread(task).start())

  private val stopGrace = 1.second

  override def processors: List[ProcessorDescriptor] = managed.map(_.descriptor)

  override def topology: ProcessingTopology =
    ProcessingTopology(identity.self, identity.replicaCount, managed.map(topologyOf))

  override def status(id: ProcessorId): ProcessorStatus = {
    val processor = processorOf(id)
    val current   = runs.get
    ProcessorStatus(id, processor.shards.flatMap(slots => current.get(ShardKey(id, slots.shard)).map(statusOf(slots, _))))
  }

  override def statistics(id: ProcessorId): ProcessorStatistics = {
    val processor = processorOf(id)
    val current   = runs.get
    ProcessorStatistics(
      id,
      processor.shards.flatMap(slots =>
        current.get(ShardKey(id, slots.shard)).map(run => ShardStatistics(statusOf(slots, run), run.shard.metrics.snapshot))
      )
    )
  }

  override def start(id: ProcessorId): Unit = control.call(keysOf(processorOf(id)).foreach(handle(_, WorkerEvent.StartRequested)))

  override def stop(id: ProcessorId): StopResult = stop(id, settings.shutdownTimeout)

  override def stop(id: ProcessorId, timeout: FiniteDuration): StopResult = {
    val processor = processorOf(id)
    control.call(resultOf(id, stopShards(keysOf(processor), ReleaseReason.Stopped, timeout)))
  }

  override def restart(id: ProcessorId): Unit =
    control.call(keysOf(processorOf(id)).foreach(handle(_, WorkerEvent.RestartRequested)))

  override def restart(id: ProcessorId, shard: Int): Unit =
    control.call(handle(ownedKey(processorOf(id), shard), WorkerEvent.RestartRequested))

  override def stopAll(timeout: FiniteDuration): List[StopResult] =
    control.call(resultsOf(stopShards(managed.flatMap(keysOf), ReleaseReason.Stopped, timeout)))

  override def onContextStarted(context: ApplicationContext): Unit = {
    managed.foreach(logTopology)
    if (settings.autostart) {
      managed.filter(_.settings.enabled).foreach(processor => start(processor.id))
    }
  }

  override def onContextClosing(): Unit = {
    val results = control.call(resultsOf(stopShards(managed.flatMap(keysOf), ReleaseReason.Shutdown, settings.shutdownTimeout)))
    results.filterNot(_.isComplete).foreach { result =>
      log.warn(
        s"Shards ${result.abandoned.mkString(", ")} of processor ${result.id.name} did not stop within ${settings.shutdownTimeout}"
      )
    }
  }

  /** Stops the control thread and closes the processors, calling their `@PreDestroy` methods. */
  @PreDestroy
  def close(): Unit = {
    control.close()
    managed.foreach(processor => guarded(s"Closing processor ${processor.id.name}")(processor.component.close()))
  }

  private def initialRuns: Map[ShardKey, ShardRun] = {
    val now = clock.instant
    managed.flatMap { processor =>
      val state = if (processor.settings.enabled) WorkerState.Idle else WorkerState.Stopped
      processor.shards.map(slots =>
        ShardKey(processor.id, slots.shard) ->
          ShardRun(ShardSupervision(state, now), shardOf(processor, slots, ShardMetrics(clock)))
      )
    }.toMap
  }

  private def processorOf(id: ProcessorId): ManagedProcessor = byId.getOrElse(id, throw NoSuchProcessorException(id))

  private def keysOf(processor: ManagedProcessor): List[ShardKey] =
    processor.shards.map(slots => ShardKey(processor.id, slots.shard))

  private def ownedKey(processor: ManagedProcessor, shard: Int): ShardKey =
    keysOf(processor).find(_.shard == shard).getOrElse(throw notOwned(processor, shard))

  private def notOwned(processor: ManagedProcessor, shard: Int): ShardNotOwnedException = {
    val count  = processor.settings.shardCount
    val owners =
      if (shard >= 0 && shard < count) processor.settings.assignment.replicasOf(shard, count, identity.replicaCount) else Nil
    ShardNotOwnedException(processor.id, shard, owners)
  }

  private def shardOf(processor: ManagedProcessor, slots: SlotSet, metrics: ShardMetrics): Shard =
    Shard(processor.id, slots, identity.self, metrics)

  private def topologyOf(processor: ManagedProcessor): ProcessorTopology = {
    val resolved = processor.settings
    ProcessorTopology(processor.id, resolved.slotCount, resolved.shardCount, resolved.assignment.name, processor.shards)
  }

  private def statusOf(slots: SlotSet, run: ShardRun): ShardStatus = {
    val supervision = run.supervision
    ShardStatus(slots, supervision.state, supervision.since, supervision.restarts, supervision.lastFailure)
  }

  private def logTopology(processor: ManagedProcessor): Unit = {
    val resolved = processor.settings
    val shards   = if (processor.shards.isEmpty) "no shards" else s"shards ${processor.shards.map(_.shard).mkString(", ")}"
    log.info(
      s"Processor ${processor.id.name} runs $shards of ${resolved.shardCount} on replica ${identity.self.index} of " +
        s"${identity.replicaCount} (${resolved.slotCount} slots, ${resolved.assignment.name}, ${processor.engine.name})"
    )
  }

  private def handle(key: ShardKey, event: WorkerEvent): Unit =
    runs.get.get(key).foreach { run =>
      val step = run.supervision.on(event, clock.instant, byId(key.id).settings.policy, ThreadLocalRandom.current.nextDouble())
      moved(key, run, step)
      step.commands.foreach(execute(key, _))
    }

  private def moved(key: ShardKey, run: ShardRun, step: SupervisionStep): Unit = {
    update(key, _.copy(supervision = step.supervision))
    val previous = run.supervision.state
    val current  = step.supervision.state
    if (previous != current)
      announce(ProcessorEvent.ShardStateChanged(key.id, key.shard, previous, current, step.supervision.since))
  }

  private def announce(event: ProcessorEvent): Unit =
    publisher.foreach(events => guarded(s"Publishing $event")(events.publish(event)))

  private def handleCurrent(key: ShardKey, generation: Long, event: WorkerEvent): Unit =
    if (runs.get.get(key).exists(_.generation == generation)) handle(key, event)

  private def update(key: ShardKey, change: ShardRun => ShardRun): Unit =
    runs.getAndUpdate(current => current.updatedWith(key)(_.map(change))): Unit

  private def runOf(key: ShardKey): ShardRun = runs.get()(key)

  private def execute(key: ShardKey, command: WorkerCommand): Unit =
    command match {
      case WorkerCommand.Acquire                => callback(key, "onAcquired")(_.onAcquired(runOf(key).shard))
      case WorkerCommand.StartWorker            => startWorker(key)
      case WorkerCommand.StopWorker             => stopWorker(key, settings.shutdownTimeout)
      case WorkerCommand.ScheduleRestart(delay) => scheduleRestart(key, delay)
      case WorkerCommand.CancelRestart          => cancelRestart(key)
      case WorkerCommand.Release(reason)        => callback(key, "onReleased")(_.onReleased(runOf(key).shard, reason))
    }

  private def callback(key: ShardKey, name: String)(call: ProcessorDefinition => Unit): Unit =
    guarded(s"$name of shard $key")(call(byId(key.id).definition))

  private def startWorker(key: ShardKey): Unit = {
    val processor  = byId(key.id)
    val generation = runOf(key).generation + 1
    val shard      = shardOf(processor, runOf(key).shard.slots, runOf(key).shard.metrics)
    update(key, _.copy(shard = shard, generation = generation, worker = None))
    try {
      val worker = processor.engine.start(processor.definition, shard, eventsOf(key, generation))
      update(key, _.copy(worker = Option(worker)))
    } catch {
      case NonFatal(error) => crashed(key, generation, FailureStage.Prepare, error)
    }
  }

  private def eventsOf(key: ShardKey, generation: Long): WorkerEvents =
    new WorkerEvents {
      override def prepared(): Unit = control.post(handleCurrent(key, generation, WorkerEvent.Prepared))

      override def finished(): Unit = control.post(handleCurrent(key, generation, WorkerEvent.Finished))

      override def crashed(stage: FailureStage, error: Throwable): Unit =
        control.post(DefaultProcessorManager.this.crashed(key, generation, stage, error))
    }

  private def crashed(key: ShardKey, generation: Long, stage: FailureStage, error: Throwable): Unit =
    if (runs.get.get(key).exists(_.generation == generation)) {
      log.warn(s"Shard $key failed in stage $stage", error)
      val failure = FailureInfo.of(stage, error, clock.instant)
      announce(ProcessorEvent.ShardFailed(key.id, key.shard, failure))
      handle(key, WorkerEvent.Crashed(failure))
    }

  private def stopWorker(key: ShardKey, timeout: FiniteDuration): Unit = {
    runOf(key).worker.foreach(worker => logAbandoned(key, worker.stop(timeout), timeout))
    update(key, _.copy(worker = None))
    handle(key, WorkerEvent.WorkerStopped)
  }

  private def logAbandoned(key: ShardKey, outcome: StopOutcome, timeout: FiniteDuration): Unit =
    if (outcome == StopOutcome.Abandoned) log.warn(s"Shard $key did not stop within $timeout and was left behind")

  private def scheduleRestart(key: ShardKey, delay: FiniteDuration): Unit = {
    val generation = runOf(key).generation
    log.info(s"Shard $key starts again in ${delay.toMillis} ms")
    val task       = scheduler.schedule(delay)(control.post(handleCurrent(key, generation, WorkerEvent.RestartDue)))
    update(key, _.copy(restart = Option(task)))
  }

  private def cancelRestart(key: ShardKey): Unit = {
    runOf(key).restart.foreach(_.cancel())
    update(key, _.copy(restart = None))
  }

  private def stopShards(keys: List[ShardKey], reason: ReleaseReason, timeout: FiniteDuration): Map[ShardKey, StopOutcome] = {
    val stopping = keys.filter(requestStop(_, reason))
    val outcomes = stoppedTogether(stopping.flatMap(key => runOf(key).worker.map(key -> _)), timeout)
    stopping.foreach { key =>
      update(key, _.copy(worker = None))
      handle(key, WorkerEvent.WorkerStopped)
    }
    keys.map(key => key -> outcomes.getOrElse(key, StopOutcome.Stopped)).toMap
  }

  private def requestStop(key: ShardKey, reason: ReleaseReason): Boolean =
    runs.get.get(key).exists { run =>
      val step = run.supervision.on(WorkerEvent.StopRequested(reason), clock.instant, byId(key.id).settings.policy, 0.5)
      moved(key, run, step)
      step.commands.filterNot(_ == WorkerCommand.StopWorker).foreach(execute(key, _))
      step.commands.contains(WorkerCommand.StopWorker)
    }

  private def stoppedTogether(workers: List[(ShardKey, RunningWorker)], timeout: FiniteDuration): Map[ShardKey, StopOutcome] = {
    val deadline = (timeout + stopGrace).fromNow
    val stops    = workers.map((key, worker) => key -> Future(worker.stop(timeout))(using stopContext))
    stops.map((key, stop) => key -> outcomeBefore(stop, deadline)).toMap
  }

  private def outcomeBefore(stop: Future[StopOutcome], deadline: Deadline): StopOutcome =
    try {
      Await.result(stop, deadline.timeLeft.max(Duration.Zero))
    } catch {
      case _: TimeoutException => StopOutcome.Abandoned
    }

  private def resultsOf(outcomes: Map[ShardKey, StopOutcome]): List[StopResult] =
    managed.map(processor => resultOf(processor.id, outcomes.filter((key, _) => key.id == processor.id)))

  private def resultOf(id: ProcessorId, outcomes: Map[ShardKey, StopOutcome]): StopResult = {
    val (abandoned, stopped) = outcomes.toList.partition(_._2 == StopOutcome.Abandoned)
    StopResult(id, stopped.map(_._1.shard).sorted, abandoned.map(_._1.shard).sorted)
  }

  private def guarded(what: String)(task: => Unit): Unit =
    try {
      task
    } catch {
      case NonFatal(error) => log.error(s"$what failed", error)
    }
}
