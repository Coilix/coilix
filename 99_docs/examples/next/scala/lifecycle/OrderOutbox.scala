package com.example
package lifecycle

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class OrderOutbox(mailer: MailSender) extends ContextLifecycleListener {
  private val pending = ConcurrentLinkedQueue[String]()

  def add(order: String): Unit = {
    val _ = pending.add(order)
  }

  override def closingPhase: ClosingPhase = ClosingPhase.Events

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit

  override def onContextClosing: IO[Unit] = IO.blocking {
    Iterator.continually(Option(pending.poll())).takeWhile(_.isDefined).flatten.foreach(mailer.send("shop@example.com", _))
  }
}
