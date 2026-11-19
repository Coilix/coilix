package dev.coilix.demo
package vehicle

import dev.coilix.demo.access.AccessError

/** A control module of a vehicle. */
trait Module {

  /** Identifier of the module in its vehicle. */
  def id: ModuleId

  /** Actions the module supports. */
  def supportedActions: Set[Action]

  /** Runs a command that the vehicle has already authorized. */
  def execute(command: ModuleCommand): Either[AccessError, ModuleResult]
}
