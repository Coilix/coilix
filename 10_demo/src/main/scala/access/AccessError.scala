package dev.coilix.demo
package access

import dev.coilix.common.validation.Violation

/** Why a vehicle or the workshop refused access. */
enum AccessError {

  /** The token was not signed by a workshop the vehicle trusts, or was changed after signing. */
  case InvalidSignature

  /** The token is no longer valid. */
  case TokenExpired

  /** The token was issued for another vehicle. */
  case WrongVehicle

  /** The token does not grant the action on the module. */
  case MissingGrant

  /** The vehicle has no such module. */
  case UnknownModule

  /** The request was already sent in this session. */
  case ReplayedRequest

  /** The module does not support the action or its parameters. */
  case ModuleRejected

  /** The vehicle is not registered with the OEM portal. */
  case UnknownVehicle

  /** The mechanic's certification does not allow the requested actions. */
  case NotCertified

  /** The access request itself is invalid. */
  case InvalidRequest(violations: List[Violation])
}
