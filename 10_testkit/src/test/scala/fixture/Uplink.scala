package dev.coilix.testkit
package fixture

trait Uplink {
  def send(message: String): Unit
}
