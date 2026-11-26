package com.example
package logging

import cats.effect.IO
import dev.coilix.annotation.Service
import dev.coilix.annotation.Slf4s

@Slf4s
@Service
final class PaymentAudit {
  def record(payment: String, amount: BigDecimal): IO[Unit] =
    logF.debug(s"Checking payment $payment") *>
      IO.raiseWhen(amount <= 0)(IllegalArgumentException(s"Payment $payment has no amount")) *>
      logF.info(s"Recorded payment $payment of $amount")

  def recordSafely(payment: String, amount: BigDecimal): IO[Unit] =
    record(payment, amount).handleErrorWith(error => logF.warn(s"Payment $payment was not recorded", error))

  def startupNote(): Unit = log.info("Payment audit is ready")
}
