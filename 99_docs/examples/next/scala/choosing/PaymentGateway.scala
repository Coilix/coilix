package com.example
package choosing

trait PaymentGateway {
  def pay(amount: BigDecimal): String
}
