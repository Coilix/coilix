package com.example
package choosing

import dev.coilix.annotation.Component
import dev.coilix.annotation.Primary

@Component
@Primary
final class CardGateway extends PaymentGateway {
  override def pay(amount: BigDecimal): String = s"card payment of $amount"
}
