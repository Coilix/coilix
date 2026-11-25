package dev.coilix.concurrency

/**
 * Runs computation on the single pool of the application that has about one thread per processor core.
 *
 * The concurrency module registers the `ioTaskExecutor` bean on the compute pool of the cats-effect runtime, which has
 * `coilix.concurrency.compute.threads` threads and closes with the context. [[dev.coilix.annotation.Async]] methods and asynchronous event listeners run on
 * it. A task that waits, for example for a database, holds a thread of this pool and a processor core with it, so such work
 * goes to the [[dev.coilix.concurrency.BlockingExecutor]] instead.
 *
 * The default bean is left out when another bean provides a `TaskExecutor`, such as an
 * [[dev.coilix.concurrency.ExecutorServiceTaskExecutor]] on a pool of its own:
 *
 * ```scala
 * @Service
 * final class ReportService(executor: TaskExecutor) {
 *   def render(id: ReportId): Future[Report] = executor.submit(renderer.render(id))
 * }
 * ```
 */
trait TaskExecutor extends ManagedExecutor
