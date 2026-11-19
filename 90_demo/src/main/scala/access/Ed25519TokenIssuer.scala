package dev.coilix.demo
package access

import dev.coilix.annotation.Component
import java.security.KeyPair
import java.security.Signature

/**
 * Signs tokens with the Ed25519 key of the workshop.
 *
 * @param workshopKeys
 *   the key pair from [[dev.coilix.demo.workshop.WorkshopConfiguration.workshopKeys]]
 */
@Component
final class Ed25519TokenIssuer(workshopKeys: KeyPair) extends TokenIssuer {

  override def issue(claims: TokenClaims): Either[AccessError, AuthToken] =
    Right(AuthToken(claims, sign(claims)))

  private def sign(claims: TokenClaims): Vector[Byte] = {
    val signer = Signature.getInstance("Ed25519")
    signer.initSign(workshopKeys.getPrivate)
    signer.update(ClaimsCodec.encode(claims))
    signer.sign().toVector
  }
}
