package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Primary

/** Original parts from the manufacturer. `@Primary`, so it is injected wherever a [[PartsSupplier]] is required. */
@Component
@Primary
final class OemPartsSupplier extends PartsSupplier {

  override def name: String = "OEM"

  override def price(part: String): BigDecimal = BigDecimal(40) + part.length
}
