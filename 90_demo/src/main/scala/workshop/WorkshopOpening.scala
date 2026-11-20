package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ContextLifecycleListener
import java.util.concurrent.atomic.AtomicBoolean

/** Opens the workshop when every bean exists and announces the closing before any bean is destroyed. */
@Component
@Slf4s
final class WorkshopOpening(properties: WorkshopProperties) extends ContextLifecycleListener {
  private val open = AtomicBoolean(false)

  /** Whether the workshop is open. */
  def isOpen: Boolean = open.get

  override def onContextStarted(context: ApplicationContext): Unit = {
    open.set(true)
    log.info(s"${properties.name} is open with ${context.beanNames.size} beans")
  }

  override def onContextClosing(): Unit = {
    open.set(false)
    log.info(s"${properties.name} is closing")
  }
}
