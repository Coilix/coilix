package dev.coilix.concurrency

/**
 * Runs computation on the single pool of the application that has about one thread per processor core.
 *
 * The concurrency module registers a `taskExecutor` bean on a `ForkJoinPool` with `coilix.concurrency.compute.threads`
 * platform threads, closed with the context. [[dev.coilix.annotation.Async]] methods and asynchronous event listeners run on
 * it. A task that waits, for example for a database, holds a thread of this pool and a processor core with it, so such work
 * goes to the [[dev.coilix.concurrency.BlockingExecutor]] instead.
 *
 * The default bean is left out when another bean provides a `TaskExecutor`, so an integration such as the cats-effect module
 * can make the compute pool of its runtime the only compute pool of the application:
 *
 * ```scala
 * @Service
 * final class ReportService(executor: TaskExecutor) {
 *   def render(id: ReportId): Future[Report] = executor.submit(renderer.render(id))
 * }
 * ```
 */
trait TaskExecutor extends ManagedExecutor
