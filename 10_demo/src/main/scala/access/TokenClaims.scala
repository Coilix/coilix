package dev.coilix.demo
package access

import dev.coilix.demo.vehicle.Vin
import java.time.Instant

/** What a token allows: who, on which vehicle, in which session, what and until when. */
final case class TokenClaims(
  mechanic:  MechanicId,
  vehicle:   Vin,
  session:   SessionId,
  grants:    Set[Grant],
  issuedAt:  Instant,
  expiresAt: Instant
)
