package dev.coilix.demo
package access

import dev.coilix.annotation.Component
import java.security.Signature
import scala.util.Try

/** Checks Ed25519 signatures with the JDK. A malformed signature counts as invalid. */
@Component
final class Ed25519TokenVerifier extends TokenVerifier {

  override def verify(token: AuthToken, key: PublicKey): Either[AccessError, Unit] =
    Either.cond(isSigned(token, key), (), AccessError.InvalidSignature)

  private def isSigned(token: AuthToken, key: PublicKey): Boolean =
    Try {
      val verifier = Signature.getInstance("Ed25519")
      verifier.initVerify(key.value)
      verifier.update(ClaimsCodec.encode(token.claims))
      verifier.verify(token.signature.toArray)
    }.getOrElse(false)
}
