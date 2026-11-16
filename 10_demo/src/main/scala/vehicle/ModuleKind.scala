package dev.coilix.demo
package vehicle

import dev.coilix.annotation.Enumerated

/** Kind of a control module, which selects the [[dev.coilix.demo.vehicle.ModuleDriver]] that talks to it. */
@Enumerated(ignoreCase = true)
enum ModuleKind {

  /** Engine control unit. */
  case Engine

  /** Anti-lock braking system. */
  case Brakes

  /** Immobilizer with the vehicle keys. */
  case Immobilizer
}
