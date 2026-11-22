package dev.coilix.concurrency

import java.util.Map as JavaMap
import org.slf4j.MDC

/** The MDC of a thread at the moment of capture, or `None` when it was empty. */
private[concurrency] final class MdcSnapshot(captured: Option[JavaMap[String, String]]) extends ContextSnapshot {

  override def runWith[T](task: => T): T = {
    val previous = Option(MDC.getCopyOfContextMap)
    install(captured)
    try {
      task
    } finally {
      install(previous)
    }
  }

  private def install(context: Option[JavaMap[String, String]]): Unit =
    context.fold(MDC.clear())(MDC.setContextMap)
}
