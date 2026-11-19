package dev.coilix.concurrency

import java.util.concurrent.ThreadFactory
import scala.util.Try

/**
 * Thread factories for the executors of Coilix.
 *
 * Coilix runs on JDK 17 or newer, while virtual threads exist from JDK 21. They are therefore created through reflection, and
 * on an older JDK the factories fall back to platform threads.
 */
object ThreadFactories {

  /** Whether the running JDK supports virtual threads. */
  def virtualThreadsAvailable: Boolean = virtualFactory("coilix-probe-").isDefined

  /**
   * A factory of threads named `<prefix><number>`, numbered from 1.
   *
   * @param virtual
   *   whether to create virtual threads, which falls back to daemon platform threads when the JDK has no virtual threads
   */
  def named(prefix: String, virtual: Boolean): ThreadFactory =
    Option.when(virtual)(virtualFactory(prefix)).flatten.getOrElse(NamedThreadFactory(prefix))

  /** Whether `thread` is a virtual thread, always `false` on a JDK without virtual threads. */
  def isVirtual(thread: Thread): Boolean =
    Try(classOf[Thread].getMethod("isVirtual").invoke(thread)).toOption.contains(java.lang.Boolean.TRUE)

  private def virtualFactory(prefix: String): Option[ThreadFactory] =
    Try {
      val builderClass = Class.forName("java.lang.Thread$Builder")
      val builder      = classOf[Thread].getMethod("ofVirtual").invoke(classOf[Thread])
      val named        = builderClass.getMethod("name", classOf[String], java.lang.Long.TYPE).invoke(builder, prefix, 1L)
      builderClass.getMethod("factory").invoke(named)
    }.toOption.collect { case factory: ThreadFactory => factory }
}
