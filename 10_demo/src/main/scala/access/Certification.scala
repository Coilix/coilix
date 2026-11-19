package dev.coilix.demo
package access

import dev.coilix.annotation.Enumerated

/** Certification level of a mechanic, which limits the actions [[dev.coilix.demo.access.CertificationAccessPolicy]] grants. */
@Enumerated(ignoreCase = true)
enum Certification {

  /** May only read faults and live data. */
  case Apprentice

  /** May also run tests and adapt modules. */
  case Technician

  /** May do everything, including coding. */
  case Master
}
