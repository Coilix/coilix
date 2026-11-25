package dev.coilix.concurrency

import cats.effect.IO

/**
 * Extensions of `IO` for applications of Coilix, imported with `import dev.coilix.concurrency.CoilixIO.*`.
 *
 * ```scala
 * @Service
 * final class RecallCheck(portal: ManufacturerPortal)(using BlockingExecutor) {
 *   def recalls(vins: List[Vin]): IO[List[Recall]] = vins.parTraverse(vin => IO.onBlockingExecutor(portal.recalls(vin)))
 * }
 * ```
 */
object CoilixIO {

  extension (io: IO.type) {

    /**
     * Runs the blocking `thunk` on a virtual thread of the [[dev.coilix.concurrency.BlockingExecutor]] and continues on the
     * compute pool afterwards.
     *
     * `IO.blocking` blocks on a platform helper thread of cats-effect, which suits a few blocking calls at a time. For thousands
     * of calls that wait at once, such as requests to a slow service, virtual threads cost less.
     *
     * The thunk is suspended with `IO.delay`, not `IO.blocking`: every thread of the blocking executor may block, and
     * `IO.blocking` would move a thread that is not virtual, such as one of the pool used on JDK 17, to the blocking pool of
     * cats-effect again.
     */
    def onBlockingExecutor[A](thunk: => A)(using blocking: BlockingExecutor): IO[A] =
      IO.delay(thunk).evalOn(blocking.executionContext)
  }
}
