package com.example
package scopes

import dev.coilix.annotation.Service
import dev.coilix.context.Provider

@Service
final class PriceDesk(catalog: Provider[ProductCatalog]) {
  def quote(product: String): BigDecimal = catalog.get().price(product)
}
