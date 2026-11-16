package dev.coilix.demo
package workshop

import dev.coilix.config.valuetype.Secret

/**
 * Connection to the OEM portal where the workshop registers its key.
 *
 * @param apiKey
 *   a [[dev.coilix.config.valuetype.Secret]], so it shows as `*****` when logged
 */
final case class OemPortalSettings(url: String, apiKey: Secret)
