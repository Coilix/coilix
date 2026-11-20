package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component

/** Cheaper replacement parts, injected only where asked for by name with `@Qualifier`. */
@Component
final class AftermarketPartsSupplier extends PartsSupplier {

  override def name: String = "aftermarket"

  override def price(part: String): BigDecimal = BigDecimal(25) + part.length
}
