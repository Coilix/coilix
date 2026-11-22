package dev.coilix.demo

import dev.coilix.demo.workshop.Notifier
import java.util.concurrent.atomic.AtomicReference

final class RecordingNotifier extends Notifier {
  private val sent = AtomicReference(List.empty[String])

  override def description: String = "recorded by the test"

  override def send(message: String): Unit = {
    val _ = sent.updateAndGet(_ :+ message)
  }

  def messages: List[String] = sent.get
}
