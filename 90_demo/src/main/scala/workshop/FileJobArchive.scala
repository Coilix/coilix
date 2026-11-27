package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Async
import dev.coilix.annotation.Service
import dev.coilix.annotation.Slf4s

/**
 * Archives reports to files. Writing a file waits for the disk, so `archive` is `@Async(blocking = true)` and runs on the
 * [[dev.coilix.concurrency.BlockingExecutor]] instead of the compute pool; its result tells the thread it ran on. `index`
 * returns `IO[Unit]`, so Coilix starts it on a fiber of its own and the caller goes on without waiting.
 */
@Service
@Slf4s
final class FileJobArchive extends JobArchive {

  @Async(blocking = true)
  override def archive(report: String): IO[String] =
    IO.pure(s"'$report' archived on ${Thread.currentThread.getName}")

  @Async
  override def index(report: String): IO[Unit] =
    logF.info(s"'$report' added to the search index")
}
