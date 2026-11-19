package dev.coilix.demo
package workshop

/** Tells the workshop staff about finished jobs. Which implementation is used depends on the active profile. */
trait Notifier {

  /** A short description of where notifications go. */
  def description: String

  /** Sends one notification. */
  def send(message: String): Unit
}
