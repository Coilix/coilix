package dev.coilix.demo
package vehicle

import dev.coilix.demo.access.AccessError
import dev.coilix.demo.workshop.ModuleSettings

/**
 * Talks to one kind of control module.
 *
 * Every driver is a bean, and [[dev.coilix.demo.vehicle.VehicleRegistry]] receives all of them as `List[ModuleDriver]`.
 */
trait ModuleDriver {

  /** The kind of module this driver talks to. */
  def kind: ModuleKind

  /** Actions this kind of module supports. */
  def supportedActions: Set[Action]

  /** Runs `command` on the module described by `settings`. */
  def execute(settings: ModuleSettings, command: ModuleCommand): Either[AccessError, ModuleResult]
}
