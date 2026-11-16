package dev.coilix.demo
package access

import java.nio.charset.StandardCharsets

/** The bytes that are signed for a token. Grants are sorted, so equal claims always give the same bytes. */
object ClaimsCodec {

  /** The signed form of `claims`. */
  def encode(claims: TokenClaims): Array[Byte] =
    List(
      claims.mechanic.value,
      claims.vehicle.value,
      claims.session.value,
      grants(claims.grants),
      claims.issuedAt.toString,
      claims.expiresAt.toString
    )
      .mkString("|")
      .getBytes(StandardCharsets.UTF_8)

  private def grants(grants: Set[Grant]): String =
    grants.toList.map(grant => s"${grant.module.value}:${grant.action}").sorted.mkString(",")
}
