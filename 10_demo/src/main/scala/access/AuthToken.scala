package dev.coilix.demo
package access

/**
 * Claims signed by a workshop.
 *
 * @param signature
 *   Ed25519 signature of [[dev.coilix.demo.access.ClaimsCodec.encode]] of the claims
 */
final case class AuthToken(claims: TokenClaims, signature: Vector[Byte])
