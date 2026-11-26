package dev.coilix.logging

/** Writes log events somewhere, such as the console. */
trait LogAppender {

  /** Writes `event`. It is called from the thread that logs, so it must not fail. */
  def append(event: LogEvent): Unit

  /** Writes what is still waiting and releases the resources of the appender. */
  def close(): Unit = ()
}
