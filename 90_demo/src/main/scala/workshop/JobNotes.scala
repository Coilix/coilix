package dev.coilix.demo
package workshop

/**
 * Notes attached to a service job.
 *
 * @param customer
 *   the note shown to the customer
 * @param internal
 *   the note for the workshop staff
 */
final case class JobNotes(customer: Option[String], internal: Option[String])
