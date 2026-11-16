package dev.coilix.demo
package vehicle

import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.AuthToken
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.PublicKeyRepository
import dev.coilix.demo.access.ReplayGuard
import dev.coilix.demo.access.TokenVerifier
import java.time.Instant

/**
 * A vehicle that checks every diagnostic command itself, so a workshop tool cannot bypass the checks.
 *
 * A command runs only when the token is signed by a trusted workshop, was issued for this vehicle, has not expired,
 * grants the action on the module, the module supports the action and the request was not sent before.
 */
trait Vehicle {

  /** Identification number of the vehicle. */
  def vin: Vin

  /** Model name, used in logs. */
  def model: String

  /** Control modules by identifier. */
  def modules: Map[ModuleId, Module]

  /** Keys of the workshops this vehicle trusts. */
  protected def keys: PublicKeyRepository

  /** Checks token signatures. */
  protected def verifier: TokenVerifier

  /** Rejects requests that were already sent. */
  protected def replayGuard: ReplayGuard

  /** Runs `command` on the module `moduleId` when `token` allows it at the time `now`. */
  def program(token: AuthToken, moduleId: ModuleId, command: ModuleCommand, now: Instant): Either[AccessError, ModuleResult] =
    for {
      key    <- keys.getKey(this)
      _      <- verifier.verify(token, key)
      _      <- check(token.claims.vehicle == vin, AccessError.WrongVehicle)
      _      <- check(token.claims.expiresAt.isAfter(now), AccessError.TokenExpired)
      _      <- check(token.claims.grants.contains(Grant(moduleId, command.action)), AccessError.MissingGrant)
      module <- modules.get(moduleId).toRight(AccessError.UnknownModule)
      _      <- check(module.supportedActions.contains(command.action), AccessError.ModuleRejected)
      _      <- replayGuard.claim(token.claims.session, command.requestId)
      result <- module.execute(command)
    } yield result

  private def check(condition: Boolean, error: AccessError): Either[AccessError, Unit] =
    Either.cond(condition, (), error)
}
