package dev.coilix.demo

/** Tells users about events. Which implementation is used depends on the active profile. */
trait Notifier {

  /** A short description of where notifications go. */
  def description: String
}
