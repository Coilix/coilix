package com.example
package events

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextClosing
import java.util.concurrent.atomic.AtomicBoolean

@Component
final class ShopStatus {
  private val open = AtomicBoolean(false)

  def isOpen: Boolean = open.get

  @EventListener
  def onApplicationStarted(event: ApplicationStarted): Unit = open.set(true)

  @EventListener
  def onContextClosing(event: ContextClosing): Unit = open.set(false)
}
