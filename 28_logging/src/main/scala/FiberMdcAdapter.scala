package dev.coilix.logging

import java.util.Deque
import java.util.Map as JavaMap
import org.slf4j.spi.MDCAdapter
import scala.jdk.CollectionConverters.*

/**
 * The MDC of the `logging` module: in a fiber the values belong to the fiber, so they follow it across threads and a fiber it
 * starts, such as an asynchronous event listener, begins with a copy of them; outside a fiber they belong to the thread, like
 * the MDC of SLF4J.
 *
 * The stacks of values by key (`pushByKey`) always belong to the thread.
 *
 * @param threads
 *   the MDC of threads that run no fiber
 * @param fibers
 *   the MDC of fibers, or `None` when cats-effect does not track the context of fibers
 */
private[logging] final class FiberMdcAdapter(threads: MDCAdapter, fibers: Option[FiberContext]) extends MDCAdapter {

  override def put(key: String, value: String): Unit =
    inFiber.fold(threads.put(key, value))(fiber => fiber.set(fiber.get.updated(key, value)))

  override def get(key: String): String =
    inFiber.fold(threads.get(key))(_.get.getOrElse(key, FiberMdcAdapter.missing))

  override def remove(key: String): Unit =
    inFiber.fold(threads.remove(key))(fiber => fiber.set(fiber.get - key))

  override def clear(): Unit =
    inFiber.fold(threads.clear())(_.set(Map.empty))

  override def getCopyOfContextMap: JavaMap[String, String] =
    inFiber.fold(threads.getCopyOfContextMap)(fiber => java.util.HashMap(fiber.get.asJava))

  override def setContextMap(contextMap: JavaMap[String, String]): Unit =
    inFiber.fold(threads.setContextMap(contextMap))(_.set(Option(contextMap).fold(Map.empty)(_.asScala.toMap)))

  override def pushByKey(key: String, value: String): Unit = threads.pushByKey(key, value)

  override def popByKey(key: String): String = threads.popByKey(key)

  override def getCopyOfDequeByKey(key: String): Deque[String] = threads.getCopyOfDequeByKey(key)

  override def clearDequeByKey(key: String): Unit = threads.clearDequeByKey(key)

  private def inFiber: Option[FiberContext] = fibers.filter(_.isActive)
}

/** Shared values of [[dev.coilix.logging.FiberMdcAdapter]]. */
private[logging] object FiberMdcAdapter {

  /** The value that SLF4J expects for a key without a value. */
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private val missing: String = null
}
