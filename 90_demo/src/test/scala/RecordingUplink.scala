package dev.coilix.demo

import dev.coilix.demo.workshop.TelematicsUplink
import java.util.concurrent.atomic.AtomicReference

final class RecordingUplink extends TelematicsUplink {
  private val sent = AtomicReference(List.empty[String])

  override def send(report: String): Unit = {
    val _ = sent.updateAndGet(_ :+ report)
  }

  def reports: List[String] = sent.get
}
