package dev.coilix.context

/**
 * A bean that reacts when the context has created all its beans and when the context starts closing.
 *
 * Modules use it for work that needs every bean, such as finding annotated methods of other beans, and to stop that work
 * before any bean is destroyed. A listener that stops new work keeps the default
 * [[dev.coilix.context.ContextLifecycleListener.closingPhase]], so it is notified before the queued work is finished:
 *
 * ```scala
 * @Component
 * final class JobStarter(jobs: JobEngine) extends ContextLifecycleListener {
 *   override def onContextStarted(context: ApplicationContext): Unit = jobs.start(context.getBeansOfType[Job])
 *
 *   override def onContextClosing(): Unit = jobs.stop()
 * }
 * ```
 */
trait ContextLifecycleListener {

  /**
   * Called once after every bean of the context is created, in the order the listeners were created. An exception closes the
   * context and stops the startup.
   */
  def onContextStarted(context: ApplicationContext): Unit

  /**
   * Called once when the context closes, in the [[dev.coilix.context.ContextLifecycleListener.closingPhase]] of this listener
   * and before any `@PreDestroy` method runs. Listeners of one phase are called in the reverse order of creation. An exception
   * is logged and does not stop the closing. Does nothing by default.
   */
  def onContextClosing(): Unit = ()

  /**
   * The phase in which [[dev.coilix.context.ContextLifecycleListener.onContextClosing]] is called. The default is
   * [[dev.coilix.context.ClosingPhase.Producers]], because a listener usually stops work it started, and that work must stop
   * before the queued work is finished and the resources are released.
   */
  def closingPhase: ClosingPhase = ClosingPhase.Producers
}
