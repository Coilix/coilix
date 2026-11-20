package com.example
package choosing

import dev.coilix.annotation.Component
import dev.coilix.annotation.Qualifier

@Component
final class Checkout(
  payments:                               PaymentGateway,
  @Qualifier("transferGateway") invoices: PaymentGateway,
  allGateways:                            List[PaymentGateway]
) {
  def payNow(amount: BigDecimal): String = payments.pay(amount)

  def payByInvoice(amount: BigDecimal): String = invoices.pay(amount)

  def gatewayCount: Int = allGateways.size
}
