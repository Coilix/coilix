package dev.coilix.demo
package workshop

/** A supplier of spare parts. */
trait PartsSupplier {

  /** The name shown in quotes. */
  def name: String

  /** The price of one part, in euro. */
  def price(part: String): BigDecimal
}
