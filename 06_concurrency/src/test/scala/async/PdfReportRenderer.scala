package dev.coilix.concurrency
package async

import cats.effect.IO
import dev.coilix.annotation.Async
import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

@Component
final class PdfReportRenderer extends ReportRenderer {
  val events: LinkedBlockingQueue[String] = LinkedBlockingQueue[String]()

  val release: CountDownLatch = CountDownLatch(1)

  @Async
  override def render(name: String): IO[String] =
    IO.pure(s"$name on ${Thread.currentThread.getName}")

  @Async
  override def announce(name: String): IO[Unit] = {
    val thread = Thread.currentThread.getName
    IO.interruptible(release.await()) *> IO(events.add(s"announced $name on $thread")).void
  }

  @Async
  override def fail(): IO[String] = throw IllegalStateException("cannot render")

  @Async
  override def failLater(): IO[Unit] = IO.raiseError(IllegalStateException("cannot announce"))

  @Async(blocking = true)
  override def archive(name: String): IO[String] =
    IO.pure(s"$name archived on ${Thread.currentThread.getName}")

  @Async
  override def hang(): IO[Unit] = IO.never.onCancel(IO(events.add("cancelled")).void)

  override def callerThread(): String = Thread.currentThread.getName

  @PostConstruct
  private def start(): Unit = events.add("started")

  @PreDestroy
  private def stop(): Unit = events.add("stopped")
}
