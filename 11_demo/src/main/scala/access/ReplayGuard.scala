package dev.coilix.demo
package access

import dev.coilix.demo.vehicle.RequestId

/** Remembers requests, so a recorded command cannot be sent again. */
trait ReplayGuard {

  /** `Right` the first time `request` is claimed in `session`, `Left(ReplayedRequest)` afterwards. */
  def claim(session: SessionId, request: RequestId): Either[AccessError, Unit]
}
