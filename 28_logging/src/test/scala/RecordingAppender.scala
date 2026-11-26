package dev.coilix.logging

import java.util.concurrent.atomic.AtomicReference

/** An appender that keeps the events it receives. */
final class RecordingAppender extends LogAppender {
  private val events = AtomicReference(Vector.empty[LogEvent])

  override def append(event: LogEvent): Unit = events.getAndUpdate(_ :+ event): Unit

  def recorded: Vector[LogEvent] = events.get

  def messages: Vector[String] = recorded.map(event => s"${event.level.label} ${event.logger} ${event.message}")

  def clear(): Unit = events.set(Vector.empty)
}
