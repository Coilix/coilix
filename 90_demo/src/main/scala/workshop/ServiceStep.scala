package dev.coilix.demo
package workshop

import dev.coilix.demo.access.Grant
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleCommand
import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.RequestId

/** One step of a service job. */
final case class ServiceStep(module: ModuleId, action: Action, description: String, parameters: Map[String, String] = Map.empty) {

  /** The grant this step needs. */
  def grant: Grant = Grant(module, action)

  /** The command for this step. */
  def command(requestId: RequestId): ModuleCommand = ModuleCommand(action, requestId, parameters)
}
