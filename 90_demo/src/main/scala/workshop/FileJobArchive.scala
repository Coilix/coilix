package dev.coilix.demo
package workshop

import dev.coilix.annotation.Async
import dev.coilix.annotation.Service
import scala.concurrent.Future

/** Archives reports on a Coilix task thread thanks to `@Async`. The result tells the thread it ran on. */
@Service
final class FileJobArchive extends JobArchive {

  @Async
  override def archive(report: String): Future[String] =
    Future.successful(s"'$report' archived on ${Thread.currentThread.getName}")
}
