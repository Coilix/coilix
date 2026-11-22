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
  def virtualThreadsAvailable: Boolean = virtualFactory("coilix-probe-", None).isDefined

  /**
   * A factory of threads named `<prefix><number>`, numbered from 1.
   *
   * @param virtual
   *   whether to create virtual threads, which falls back to daemon platform threads when the JDK has no virtual threads
   * @param uncaught
   *   receives the exceptions that end a thread, such as fatal errors; without it the JVM default applies
   */
  def named(prefix: String, virtual: Boolean, uncaught: Option[Thread.UncaughtExceptionHandler] = None): ThreadFactory =
    Option.when(virtual)(virtualFactory(prefix, uncaught)).flatten.getOrElse(NamedThreadFactory(prefix, uncaught))

  /** Whether `thread` is a virtual thread, always `false` on a JDK without virtual threads. */
  def isVirtual(thread: Thread): Boolean =
    Try(classOf[Thread].getMethod("isVirtual").invoke(thread)).toOption.contains(java.lang.Boolean.TRUE)

  private def virtualFactory(prefix: String, uncaught: Option[Thread.UncaughtExceptionHandler]): Option[ThreadFactory] =
    Try {
      val builderClass = Class.forName("java.lang.Thread$Builder")
      val builder      = classOf[Thread].getMethod("ofVirtual").invoke(classOf[Thread])
      val named        = builderClass.getMethod("name", classOf[String], java.lang.Long.TYPE).invoke(builder, prefix, 1L)
      val handled      = uncaught.fold(named)(handler => withHandler(builderClass, named, handler))
      builderClass.getMethod("factory").invoke(handled)
    }.toOption.collect { case factory: ThreadFactory => factory }

  private def withHandler(builderClass: Class[?], builder: AnyRef, handler: Thread.UncaughtExceptionHandler): AnyRef =
    builderClass.getMethod("uncaughtExceptionHandler", classOf[Thread.UncaughtExceptionHandler]).invoke(builder, handler)
}
