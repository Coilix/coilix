package dev.coilix.concurrency

/**
 * Runs tasks that spend most of their time waiting, such as JDBC queries, HTTP calls and file access, without holding a thread
 * of the compute pool.
 *
 * The concurrency module registers a `blockingExecutor` bean that runs each task on a new virtual thread, or on a pool of
 * platform threads on a JDK without virtual threads or with `coilix.concurrency.blocking.virtual-threads = false`.
 * `coilix.concurrency.blocking.max-concurrency` limits how many tasks run at once, for example to the size of a connection
 * pool. Computation does not belong here: virtual threads share carrier threads, one per core, and a task that computes holds
 * its carrier.
 *
 * [[dev.coilix.annotation.Async]] methods with `blocking = true` and asynchronous event listeners with `blocking = true` run on
 * it:
 *
 * ```scala
 * @Service
 * final class InvoiceArchive(blocking: BlockingExecutor, storage: FileStorage) {
 *   def store(invoice: Invoice): Future[Path] = blocking.submit(storage.write(invoice.number, invoice.pdf))
 * }
 * ```
 */
trait BlockingExecutor extends ManagedExecutor
