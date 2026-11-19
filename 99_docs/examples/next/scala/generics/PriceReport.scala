package com.example
package generics

import dev.coilix.annotation.Component

@Component
final class PriceReport(current: PriceSource[Option], history: PriceSource[List]) {
  def describe(product: String): String = {
    val now     = current.price(product).fold("unknown")(_.toString)
    val earlier = history.price(product).mkString(", ")
    s"$product: $now (earlier $earlier)"
  }
}
