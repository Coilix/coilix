package com.example
package scopes

import dev.coilix.annotation.Lazy
import dev.coilix.annotation.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
@Lazy
final class ProductCatalog {
  ProductCatalog.loads.incrementAndGet()

  def price(product: String): BigDecimal = BigDecimal(product.length)
}

object ProductCatalog {
  val loads: AtomicInteger = AtomicInteger()
}
