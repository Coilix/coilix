package dev.coilix.demo
package access

/** A person who requests access to vehicles. Bound from `workshop.mechanics` in the configuration. */
final case class Mechanic(id: MechanicId, name: String, certification: Certification)
