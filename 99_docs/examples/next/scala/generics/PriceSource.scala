package com.example
package generics

trait PriceSource[F[_]] {
  def price(product: String): F[BigDecimal]
}
