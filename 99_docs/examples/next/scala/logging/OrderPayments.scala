package com.example
package logging

import cats.effect.IO
import dev.coilix.annotation.Service
import org.slf4j.MDC

@Service
final class OrderPayments(audit: PaymentAudit) {
  def pay(order: String, amount: BigDecimal): IO[Unit] =
    IO(MDC.put("order", order)) *> audit.record(s"P-$order", amount).guarantee(IO(MDC.remove("order")))

  def orderSeenAfterBlocking(order: String): IO[Option[String]] =
    IO(MDC.put("order", order)) *> IO.blocking(Option(MDC.get("order"))).guarantee(IO(MDC.remove("order")))
}
