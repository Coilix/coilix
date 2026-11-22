package dev.coilix.demo
package workshop

import dev.coilix.annotation.Async
import dev.coilix.annotation.Service
import scala.concurrent.Future

/**
 * Archives reports to files. Writing a file waits for the disk, so the method is `@Async(blocking = true)` and runs on the
 * [[dev.coilix.concurrency.BlockingExecutor]] instead of the compute pool. The result tells the thread it ran on.
 */
@Service
final class FileJobArchive extends JobArchive {

  @Async(blocking = true)
  override def archive(report: String): Future[String] =
    Future.successful(s"'$report' archived on ${Thread.currentThread.getName}")
}
