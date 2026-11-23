package com.example
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.DependsOn

@Component
@DependsOn(Array("schemaMigrations"))
class PriceCache(table: PriceTable) {
  private val prices = table.all

  def price(product: String): Option[BigDecimal] = prices.get(product)
}
