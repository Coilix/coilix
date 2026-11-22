package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Prototype
import java.util.concurrent.atomic.AtomicReference

/**
 * The sheet of one inspection, on which the mechanic notes the result of each step.
 *
 * It is a `@Prototype`, so every inspection starts with an empty sheet: [[dev.coilix.demo.workshop.WorkshopShift]] takes a new
 * one from its `Provider[InspectionSheet]` for each vehicle instead of sharing one sheet for the whole shift.
 */
@Component
@Prototype
final class InspectionSheet {
  private val entries = AtomicReference(List.empty[String])

  /** Notes the result of one step. */
  def note(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  /** The results noted so far, in order. */
  def notes: List[String] = entries.get
}
