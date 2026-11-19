package dev.coilix.demo
package access

/** Signs approved claims. */
trait TokenIssuer {

  /** A token with `claims` and their signature. */
  def issue(claims: TokenClaims): Either[AccessError, AuthToken]
}
