package dev.coilix.processing

import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * A failure of the work on a shard, kept as text so that it can be shown and sent elsewhere.
 *
 * @param stage
 *   where the work failed
 * @param errorClass
 *   the name of the class of the exception
 * @param message
 *   the message of the exception, empty when it has none
 * @param stackTrace
 *   the stack trace of the exception
 * @param at
 *   when the work failed
 */
final case class FailureInfo(stage: FailureStage, errorClass: String, message: String, stackTrace: String, at: Instant)

/** Creates failure descriptions. */
object FailureInfo {

  /** Describes the exception thrown at the given stage. */
  def of(stage: FailureStage, error: Throwable, at: Instant): FailureInfo =
    FailureInfo(stage, error.getClass.getName, Option(error.getMessage).getOrElse(""), stackTraceOf(error), at)

  private def stackTraceOf(error: Throwable): String = {
    val text = StringWriter()
    error.printStackTrace(PrintWriter(text))
    text.toString
  }
}
