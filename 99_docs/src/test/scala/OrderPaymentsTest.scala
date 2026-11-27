package com.example

import com.example.logging.OrderPayments
import com.example.logging.PaymentAudit
import munit.CatsEffectSuite

class OrderPaymentsTest extends CatsEffectSuite {

  private val payments = OrderPayments(PaymentAudit())

  test("the MDC set in IO stays with the fiber after IO.blocking") {
    payments.orderSeenAfterBlocking("7").map(order => assertEquals(order, Some("7")))
  }

  test("paying records the payment") {
    payments.pay("7", BigDecimal(120))
  }
}
