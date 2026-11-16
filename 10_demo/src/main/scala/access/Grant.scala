package dev.coilix.demo
package access

import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleId

/** Permission to run `action` on the module `module`. */
final case class Grant(module: ModuleId, action: Action)
