package dev.coilix.context

/**
 * The phase of closing in which a [[dev.coilix.context.ContextLifecycleListener]] is notified.
 *
 * The context goes through the phases in the order they are declared and runs `@PreDestroy` methods only after the last one.
 * The phases make the order of closing independent of the order in which beans were created: the application learns about the
 * closing first, whatever brings new work in stops next, the work already accepted finishes next, and the shared resources go last.
 *
 * ```scala
 * @Component
 * case class OutboxFlusher(outbox: Outbox) extends ContextLifecycleListener {
 *   override def closingPhase: ClosingPhase = ClosingPhase.Events
 *
 *   override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit
 *
 *   override def onContextClosing: IO[Unit] = outbox.flush
 * }
 * ```
 */
enum ClosingPhase {

  /**
   * Tells the application that the context is closing, before anything stops, for example by publishing the `ContextClosing`
   * application event.
   */
  case Announcement

  /** Stops the sources of new work, such as `@Scheduled` methods, message consumers and servers. */
  case Producers

  /** Finishes the work already accepted, such as delivering queued application events. */
  case Events

  /** Releases what the work needed, such as connections, executors and schedulers. */
  case Resources
}
