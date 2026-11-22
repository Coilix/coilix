package dev.coilix.concurrency

import org.slf4j.MDC

/**
 * Carries the SLF4J MDC of the submitting thread to the task, so log lines of the task keep keys such as a request id.
 *
 * The concurrency module registers it as the `mdcContextPropagator` bean unless `coilix.concurrency.propagate-mdc` is `false`.
 * The thread that runs the task gets its own MDC back afterwards, so nothing leaks to the next task of a pooled thread.
 */
final class MdcContextPropagator extends ContextPropagator {

  override def capture(): ContextSnapshot = MdcSnapshot(Option(MDC.getCopyOfContextMap))
}
