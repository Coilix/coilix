package com.example
package events

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import java.util.concurrent.atomic.AtomicReference

@Component
final class ConfirmationMailer {
  private val sent = AtomicReference(List.empty[String])

  def sentMails: List[String] = sent.get

  @EventListener(async = true)
  def onOrderPlaced(event: OrderPlaced): IO[Unit] =
    IO.blocking(sent.updateAndGet(_ :+ s"Order ${event.orderId} confirmed for ${event.customer}")).void
}
