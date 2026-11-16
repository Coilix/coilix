package dev.coilix.demo
package access

/** Checks that a token was signed with a key. */
trait TokenVerifier {

  /** `Right` when `token` was signed with the private key of `key` and not changed since. */
  def verify(token: AuthToken, key: PublicKey): Either[AccessError, Unit]
}
