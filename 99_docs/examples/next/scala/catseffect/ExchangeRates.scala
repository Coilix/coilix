package com.example
package catseffect

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.Service
import dev.coilix.concurrency.BlockingExecutor
import dev.coilix.concurrency.CoilixIO.*

@Service
final class ExchangeRates(using BlockingExecutor) {
  def rates(currencies: List[String]): IO[Map[String, BigDecimal]] =
    currencies.parTraverse(currency => IO.onBlockingExecutor(currency -> fetchRate(currency))).map(_.toMap)

  private def fetchRate(currency: String): BigDecimal =
    BigDecimal(currency.map(_.toInt).sum) / 100
}
