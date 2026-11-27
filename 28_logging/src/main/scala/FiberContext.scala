package dev.coilix.logging

import cats.effect.IO
import cats.effect.IOLocal
import java.lang.reflect.Method
import scala.util.Try

/**
 * The MDC of the current fiber, kept in an `IOLocal`, so a fiber started by another fiber begins with a copy of its MDC.
 *
 * It needs the fiber context tracking of cats-effect, enabled by the system property `cats.effect.trackFiberContext=true`,
 * which `CoilixIOApp` of the boot module sets. Whether the current thread runs a fiber is asked through
 * `cats.effect.IOFiber.currentIOFiber`, which cats-effect does not expose to Scala code, so it is called by reflection.
 *
 * @param local
 *   the value of the `IOLocal` in the fiber that runs on the current thread
 * @param currentFiber
 *   `cats.effect.IOFiber.currentIOFiber`, which gives the fiber that runs on the current thread or `null`
 */
private[logging] final class FiberContext(local: ThreadLocal[Map[String, String]], currentFiber: Method) {

  /** Whether the current thread runs a fiber, whose MDC this context holds. */
  def isActive: Boolean = Try(Option(currentFiber.invoke(FiberContext.noReceiver))).toOption.flatten.isDefined

  /** The MDC of the current fiber. */
  def get: Map[String, String] = local.get

  /** Replaces the MDC of the current fiber. */
  def set(values: Map[String, String]): Unit = local.set(values)
}

/** Creation of the [[dev.coilix.logging.FiberContext]]. */
private[logging] object FiberContext {

  /** The receiver of a static method called by reflection. */
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private val noReceiver: AnyRef = null

  /** The context of fibers, or `None` when cats-effect does not track the context of fibers. */
  def create(): Option[FiberContext] =
    if (IOLocal.isPropagating) Try(FiberContext(threadLocal(), currentFiberMethod())).toOption
    else None

  private def threadLocal(): ThreadLocal[Map[String, String]] =
    IOLocal(Map.empty[String, String]).syncStep(Int.MaxValue).unsafeRunSync() match {
      case Right(local) => local.unsafeThreadLocal()
      case Left(_)      => throw IllegalStateException("The IOLocal of the MDC needs an asynchronous step to be created")
    }

  private def currentFiberMethod(): Method =
    Class.forName("cats.effect.IOFiber").getMethod("currentIOFiber")
}
