package dev.coilix.context

/**
 * The phase of closing in which a [[dev.coilix.context.ContextLifecycleListener]] is notified.
 *
 * The context goes through the phases in the order they are declared and runs `@PreDestroy` methods only after the last one.
 * The phases make the order of closing independent of the order in which beans were created: whatever brings new work in stops
 * first, the work already accepted finishes next, and the shared resources go last.
 *
 * ```scala
 * @Component
 * final class OutboxFlusher(outbox: Outbox) extends ContextLifecycleListener {
 *   override def closingPhase: ClosingPhase = ClosingPhase.Events
 *
 *   override def onContextStarted(context: ApplicationContext): Unit = ()
 *
 *   override def onContextClosing(): Unit = outbox.flush()
 * }
 * ```
 */
enum ClosingPhase {

  /** Stops the sources of new work, such as `@Scheduled` methods, message consumers and servers. */
  case Producers

  /** Finishes the work already accepted, such as delivering queued application events. */
  case Events

  /** Releases what the work needed, such as connections, executors and schedulers. */
  case Resources
}
