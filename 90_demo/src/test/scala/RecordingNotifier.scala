package dev.coilix.demo

import dev.coilix.demo.workshop.Notifier
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.MDC

final class RecordingNotifier extends Notifier {
  private val sent = AtomicReference(List.empty[String])

  private val sentJobs = AtomicReference(List.empty[Option[String]])

  override def description: String = "recorded by the test"

  override def send(message: String): Unit = {
    val _ = sent.updateAndGet(_ :+ message)
    val _ = sentJobs.updateAndGet(_ :+ Option(MDC.get("job")))
  }

  def jobs: List[Option[String]] = sentJobs.get

  def messages: List[String] = sent.get
}
