package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Qualifier

/**
 * Compares the price of a part from two suppliers: the primary one and the one named by `@Qualifier`.
 *
 * @param primary
 *   the [[dev.coilix.demo.workshop.OemPartsSupplier]], chosen by `@Primary`
 * @param alternative
 *   the [[dev.coilix.demo.workshop.AftermarketPartsSupplier]], chosen by name
 */
@Component
final class PartsQuote(primary: PartsSupplier, @Qualifier("aftermarketPartsSupplier") alternative: PartsSupplier) {

  /** Both prices of `part`, such as `brake pads: OEM 50, aftermarket 35`. */
  def compare(part: String): String =
    s"$part: ${primary.name} ${primary.price(part)}, ${alternative.name} ${alternative.price(part)}"
}
