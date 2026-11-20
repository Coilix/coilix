package com.example
package choosing

import dev.coilix.annotation.Component

@Component
final class TransferGateway extends PaymentGateway {
  override def pay(amount: BigDecimal): String = s"bank transfer of $amount"
}
