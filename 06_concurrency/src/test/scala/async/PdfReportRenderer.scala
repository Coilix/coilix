package dev.coilix.concurrency
package async

import dev.coilix.annotation.Async
import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import java.util.concurrent.LinkedBlockingQueue
import scala.concurrent.Future

@Component
final class PdfReportRenderer extends ReportRenderer {
  val events: LinkedBlockingQueue[String] = LinkedBlockingQueue[String]()

  @Async
  override def render(name: String): Future[String] =
    Future.successful(s"$name on ${Thread.currentThread.getName}")

  @Async
  override def announce(name: String): Unit = events.add(s"announced $name on ${Thread.currentThread.getName}")

  @Async
  override def fail(): Future[String] = throw IllegalStateException("cannot render")

  override def callerThread(): String = Thread.currentThread.getName

  @PostConstruct
  private def start(): Unit = events.add("started")

  @PreDestroy
  private def stop(): Unit = events.add("stopped")
}
