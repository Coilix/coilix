package dev.coilix.demo
package workshop

/** Sends job reports to the vehicle manufacturer. The demo has no implementation, so it is injected as `None`. */
trait TelematicsUplink {

  /** Sends a job report. */
  def send(report: String): Unit
}
