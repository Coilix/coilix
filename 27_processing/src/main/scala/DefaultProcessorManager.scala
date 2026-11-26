package dev.coilix.processing

import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import cats.effect.std.Supervisor
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.cluster.ReplicaIdentity
import dev.coilix.context.ApplicationContext
import dev.coilix.context.CollectedComponents
import dev.coilix.context.ContextLifecycleListener
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * The [[dev.coilix.processing.ProcessorManager]] of the `processing` module, created as the `processorManager` bean.
 *
 * It creates the processors when it is created, starts the enabled ones when the context has started and
 * `coilix.processors.autostart` is true, and stops every shard in the first phase of closing, within
 * `coilix.processors.shutdown-timeout`, logging the shards that did not stop in time. Operations run one at a time on its
 * [[dev.coilix.processing.ControlLoop]], and the state of the shards is in a `Ref`, so reading it never waits.
 */
@Slf4s
case class DefaultProcessorManager(
  managed:    List[ManagedProcessor],
  identity:   ReplicaIdentity,
  settings:   ProcessingSettings,
  runs:       Ref[IO, Map[ShardKey, ShardRun]],
  control:    ControlLoop,
  supervisor: Supervisor[IO],
  runtime:    IORuntime
) extends ProcessorManager
    with ContextLifecycleListener {

  private val byId: Map[ProcessorId, ManagedProcessor] = managed.map(processor => processor.id -> processor).toMap

  private val stopGrace = 1.second

  override def processors: List[ProcessorDescriptor] = managed.map(_.descriptor)

  override def topology: ProcessingTopology =
    ProcessingTopology(identity.self, identity.replicaCount, managed.map(topologyOf))

  override def status(id: ProcessorId): IO[ProcessorStatus] =
    processorOf(id).flatMap(processor => runs.get.map(current => statusOf(processor, current)))

  override def start(id: ProcessorId): IO[Unit] =
    processorOf(id).flatMap(processor => control.call(keysOf(processor).traverse_(handle(_, WorkerEvent.StartRequested))))

  override def stop(id: ProcessorId): IO[StopResult] = stop(id, settings.shutdownTimeout)

  override def stop(id: ProcessorId, timeout: FiniteDuration): IO[StopResult] =
    processorOf(id).flatMap { processor =>
      control.call(stopShards(keysOf(processor), ReleaseReason.Stopped, timeout).map(resultOf(id, _)))
    }

  override def restart(id: ProcessorId): IO[Unit] =
    processorOf(id).flatMap(processor => control.call(keysOf(processor).traverse_(handle(_, WorkerEvent.RestartRequested))))

  override def restart(id: ProcessorId, shard: Int): IO[Unit] =
    processorOf(id).flatMap(ownedKey(_, shard)).flatMap(key => control.call(handle(key, WorkerEvent.RestartRequested)))

  override def stopAll(timeout: FiniteDuration): IO[List[StopResult]] =
    control.call(stopShards(managed.flatMap(keysOf), ReleaseReason.Stopped, timeout).map(resultsOf))

  /** Logs the topology and starts the enabled processors when `coilix.processors.autostart` is true. */
  def startEnabled: IO[Unit] =
    managed.traverse_(logTopology) *>
      IO.whenA(settings.autostart)(managed.filter(_.settings.enabled).traverse_(processor => start(processor.id)))

  /** Stops every shard for the shutdown, logging the shards that did not stop within the limit. */
  def stopForShutdown: IO[Unit] =
    control
      .call(stopShards(managed.flatMap(keysOf), ReleaseReason.Shutdown, settings.shutdownTimeout).map(resultsOf))
      .flatMap(_.filterNot(_.isComplete).traverse_(logIncomplete))

  override def onContextStarted(context: ApplicationContext): Unit =
    startEnabled.unsafeRunSync()(using runtime)

  override def onContextClosing(): Unit =
    stopForShutdown.unsafeRunSync()(using runtime)

  private def logIncomplete(result: StopResult): IO[Unit] =
    logF.warn(
      s"Shards ${result.abandoned.mkString(", ")} of processor ${result.id.name} did not stop within ${settings.shutdownTimeout}"
    )

  private def processorOf(id: ProcessorId): IO[ManagedProcessor] =
    IO.fromOption(byId.get(id))(NoSuchProcessorException(id))

  private def keysOf(processor: ManagedProcessor): List[ShardKey] =
    processor.shards.map(slots => ShardKey(processor.id, slots.shard))

  private def ownedKey(processor: ManagedProcessor, shard: Int): IO[ShardKey] =
    IO.fromOption(keysOf(processor).find(_.shard == shard))(notOwned(processor, shard))

  private def notOwned(processor: ManagedProcessor, shard: Int): ShardNotOwnedException = {
    val count  = processor.settings.shardCount
    val owners =
      if (shard >= 0 && shard < count) processor.settings.assignment.replicasOf(shard, count, identity.replicaCount) else Nil
    ShardNotOwnedException(processor.id, shard, owners)
  }

  private def topologyOf(processor: ManagedProcessor): ProcessorTopology = {
    val resolved = processor.settings
    ProcessorTopology(processor.id, resolved.slotCount, resolved.shardCount, resolved.assignment.name, processor.shards)
  }

  private def statusOf(processor: ManagedProcessor, current: Map[ShardKey, ShardRun]): ProcessorStatus =
    ProcessorStatus(
      processor.id,
      processor.shards.flatMap(slots => current.get(ShardKey(processor.id, slots.shard)).map(shardStatusOf(slots, _)))
    )

  private def shardStatusOf(slots: SlotSet, run: ShardRun): ShardStatus = {
    val supervision = run.supervision
    ShardStatus(slots, supervision.state, supervision.since, supervision.restarts, supervision.lastFailure)
  }

  private def logTopology(processor: ManagedProcessor): IO[Unit] = {
    val resolved = processor.settings
    val shards   = if (processor.shards.isEmpty) "no shards" else s"shards ${processor.shards.map(_.shard).mkString(", ")}"
    logF.info(
      s"Processor ${processor.id.name} runs $shards of ${resolved.shardCount} on replica ${identity.self.index} of " +
        s"${identity.replicaCount} (${resolved.slotCount} slots, ${resolved.assignment.name}, ${processor.engine.name})"
    )
  }

  private def handle(key: ShardKey, event: WorkerEvent): IO[Unit] =
    runs.get.map(_.get(key)).flatMap(_.traverse_(supervise(key, _, event)))

  private def supervise(key: ShardKey, run: ShardRun, event: WorkerEvent): IO[Unit] =
    for {
      now    <- IO.realTimeInstant
      random <- IO(ThreadLocalRandom.current.nextDouble())
      step    = run.supervision.on(event, now, byId(key.id).settings.policy, random)
      _      <- update(key, _.copy(supervision = step.supervision))
      _      <- step.commands.traverse_(execute(key, _))
    } yield ()

  private def isCurrent(key: ShardKey, generation: Long): IO[Boolean] =
    runs.get.map(_.get(key).exists(_.generation == generation))

  private def handleCurrent(key: ShardKey, generation: Long, event: WorkerEvent): IO[Unit] =
    isCurrent(key, generation).ifM(handle(key, event), IO.unit)

  private def update(key: ShardKey, change: ShardRun => ShardRun): IO[Unit] =
    runs.update(_.updatedWith(key)(_.map(change)))

  private def runOf(key: ShardKey): IO[ShardRun] = runs.get.map(_(key))

  private def execute(key: ShardKey, command: WorkerCommand): IO[Unit] =
    command match {
      case WorkerCommand.Acquire                => callback(key, "onAcquired")(_.onAcquired(_))
      case WorkerCommand.StartWorker            => startWorker(key)
      case WorkerCommand.StopWorker             => stopWorker(key, settings.shutdownTimeout)
      case WorkerCommand.ScheduleRestart(delay) => scheduleRestart(key, delay)
      case WorkerCommand.CancelRestart          => cancelRestart(key)
      case WorkerCommand.Release(reason)        => callback(key, "onReleased")(_.onReleased(_, reason))
    }

  private def callback(key: ShardKey, name: String)(call: (ProcessorDefinition, Shard) => IO[Unit]): IO[Unit] =
    guarded(s"$name of shard $key")(runOf(key).flatMap(run => call(byId(key.id).definition, run.shard)))

  private def startWorker(key: ShardKey): IO[Unit] =
    for {
      run       <- runOf(key)
      processor  = byId(key.id)
      generation = run.generation + 1
      shard     <- Shard.create(processor.id, run.shard.slots, identity.self)
      _         <- update(key, _.copy(shard = shard, generation = generation, worker = None))
      started   <- control.outside(processor.engine.start(processor.definition, shard, eventsOf(key, generation))).attempt
      _         <- started.fold(crashed(key, generation, FailureStage.Prepare, _), keepWorker(key, _))
    } yield ()

  private def keepWorker(key: ShardKey, worker: RunningWorker): IO[Unit] =
    update(key, _.copy(worker = Option(worker)))

  private def eventsOf(key: ShardKey, generation: Long): WorkerEvents =
    new WorkerEvents {
      override def prepared: IO[Unit] = control.post(handleCurrent(key, generation, WorkerEvent.Prepared))

      override def finished: IO[Unit] = control.post(handleCurrent(key, generation, WorkerEvent.Finished))

      override def crashed(stage: FailureStage, error: Throwable): IO[Unit] =
        control.post(DefaultProcessorManager.this.crashed(key, generation, stage, error))
    }

  private def crashed(key: ShardKey, generation: Long, stage: FailureStage, error: Throwable): IO[Unit] =
    isCurrent(key, generation).ifM(reportCrash(key, stage, error), IO.unit)

  private def reportCrash(key: ShardKey, stage: FailureStage, error: Throwable): IO[Unit] =
    logF.warn(s"Shard $key failed in stage $stage", error) *>
      IO.realTimeInstant.flatMap(now => handle(key, WorkerEvent.Crashed(FailureInfo.of(stage, error, now))))

  private def stopWorker(key: ShardKey, timeout: FiniteDuration): IO[Unit] =
    runOf(key).flatMap(_.worker.traverse_(_.stop(timeout).flatMap(logAbandoned(key, _, timeout)))) *>
      update(key, _.copy(worker = None)) *>
      handle(key, WorkerEvent.WorkerStopped)

  private def logAbandoned(key: ShardKey, outcome: StopOutcome, timeout: FiniteDuration): IO[Unit] =
    IO.whenA(outcome == StopOutcome.Abandoned)(logF.warn(s"Shard $key did not stop within $timeout and was left behind"))

  private def scheduleRestart(key: ShardKey, delay: FiniteDuration): IO[Unit] =
    for {
      run   <- runOf(key)
      _     <- logF.info(s"Shard $key starts again in ${delay.toMillis} ms")
      due    = IO.sleep(delay) *> control.post(handleCurrent(key, run.generation, WorkerEvent.RestartDue))
      fiber <- control.outside(supervisor.supervise(due))
      _     <- update(key, _.copy(restart = Option(fiber)))
    } yield ()

  private def cancelRestart(key: ShardKey): IO[Unit] =
    runOf(key).flatMap(_.restart.traverse_(_.cancel)) *> update(key, _.copy(restart = None))

  private def stopShards(keys: List[ShardKey], reason: ReleaseReason, timeout: FiniteDuration): IO[Map[ShardKey, StopOutcome]] =
    for {
      stopping <- keys.filterA(requestStop(_, reason))
      workers  <- stopping.flatTraverse(key => runOf(key).map(_.worker.map(key -> _).toList))
      outcomes <- workers.parTraverse((key, worker) => stopped(worker, timeout).map(key -> _))
      _        <- stopping.traverse_(key => update(key, _.copy(worker = None)) *> handle(key, WorkerEvent.WorkerStopped))
    } yield keys.map(key => key -> outcomes.toMap.getOrElse(key, StopOutcome.Stopped)).toMap

  private def stopped(worker: RunningWorker, timeout: FiniteDuration): IO[StopOutcome] =
    worker.stop(timeout).timeoutTo(timeout + stopGrace, IO.pure(StopOutcome.Abandoned))

  private def requestStop(key: ShardKey, reason: ReleaseReason): IO[Boolean] =
    runs.get.map(_.get(key)).flatMap(_.fold(IO.pure(false))(stopStep(key, _, reason)))

  private def stopStep(key: ShardKey, run: ShardRun, reason: ReleaseReason): IO[Boolean] =
    for {
      now <- IO.realTimeInstant
      step = run.supervision.on(WorkerEvent.StopRequested(reason), now, byId(key.id).settings.policy, 0.5)
      _   <- update(key, _.copy(supervision = step.supervision))
      _   <- step.commands.filterNot(_ == WorkerCommand.StopWorker).traverse_(execute(key, _))
    } yield step.commands.contains(WorkerCommand.StopWorker)

  private def resultsOf(outcomes: Map[ShardKey, StopOutcome]): List[StopResult] =
    managed.map(processor => resultOf(processor.id, outcomes.filter((key, _) => key.id == processor.id)))

  private def resultOf(id: ProcessorId, outcomes: Map[ShardKey, StopOutcome]): StopResult = {
    val (abandoned, stopped) = outcomes.toList.partition(_._2 == StopOutcome.Abandoned)
    StopResult(id, stopped.map(_._1.shard).sorted, abandoned.map(_._1.shard).sorted)
  }

  private def guarded(what: String)(task: IO[Unit]): IO[Unit] =
    task.handleErrorWith(error => logF.error(s"$what failed", error))
}

/** Creation of the [[dev.coilix.processing.DefaultProcessorManager]]. */
@Slf4s
object DefaultProcessorManager {

  /**
   * A manager whose processors are created when the resource is acquired and closed, with their `@PreDestroy` methods,
   * when it is released.
   */
  def resource(
    collected: CollectedComponents,
    engines:   List[ProcessorEngine],
    identity:  ReplicaIdentity,
    settings:  ProcessingSettings,
    runtime:   IORuntime
  ): Resource[IO, DefaultProcessorManager] =
    for {
      managed    <- Resource.make(IO.blocking(ProcessorAssembler(collected, engines, identity, settings).assemble()))(closed)
      supervisor <- Supervisor[IO](await = false)
      control    <- ControlLoop.resource
      runs       <- Resource.eval(initialRuns(managed, identity).flatMap(Ref.of[IO, Map[ShardKey, ShardRun]]))
    } yield DefaultProcessorManager(managed, identity, settings, runs, control, supervisor, runtime)

  private def closed(managed: List[ManagedProcessor]): IO[Unit] =
    managed.traverse_(processor =>
      IO.blocking(processor.component.close())
        .handleErrorWith(error => logF.error(s"Closing processor ${processor.id.name} failed", error))
    )

  private def initialRuns(managed: List[ManagedProcessor], identity: ReplicaIdentity): IO[Map[ShardKey, ShardRun]] =
    IO.realTimeInstant.flatMap(now => managed.flatTraverse(runsOf(_, identity, now))).map(_.toMap)

  private def runsOf(
    processor: ManagedProcessor,
    identity:  ReplicaIdentity,
    now:       Instant
  ): IO[List[(ShardKey, ShardRun)]] = {
    val state = if (processor.settings.enabled) WorkerState.Idle else WorkerState.Stopped
    processor.shards.traverse { slots =>
      Shard.create(processor.id, slots, identity.self).map(shard =>
        ShardKey(processor.id, slots.shard) -> ShardRun(ShardSupervision(state, now), shard)
      )
    }
  }
}
