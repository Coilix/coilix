package dev.coilix.demo
package workshop

import dev.coilix.common.edit.EditOption

/**
 * A partial update of [[dev.coilix.demo.workshop.JobNotes]]: each field is left as it is (`NoEdit`), set (`SetTo(Some(...))`)
 * or cleared (`SetTo(None)`).
 */
final case class JobNotesChange(
  customer: EditOption[String] = EditOption.NoEdit,
  internal: EditOption[String] = EditOption.NoEdit
) {

  /** `notes` with this change applied. */
  def applyTo(notes: JobNotes): JobNotes =
    JobNotes(edited(customer, notes.customer), edited(internal, notes.internal))

  private def edited(change: EditOption[String], current: Option[String]): Option[String] =
    change.fold(current, None)(Some(_))
}
