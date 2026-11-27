package dev.coilix.config
package valuetype

import dev.coilix.annotation.ValueType

/**
 * A text that must not appear in logs, such as a password or an API token.
 *
 * `toString` returns `*****`, so the secret stays hidden when the object holding it is logged or shown in an error. Read
 * the text with `value` only where it is needed.
 *
 * ```scala
 * import dev.coilix.config.valuetype.Secret
 *
 * @ConfigurationProperties("shop.payments")
 * final case class PaymentSettings(url: String, apiKey: Secret)
 * ```
 *
 * @param value
 *   the secret text
 */
@ValueType
case class Secret(value: String) {

  /** Always `*****`, so that the text is not shown. */
  override def toString: String =
    Secret.Masked
}

/** Constants for [[dev.coilix.config.valuetype.Secret]]. */
object Secret {

  /** The text shown instead of a secret. */
  val Masked: String = "*****"
}
