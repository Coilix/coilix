package com.example
package lifecycle

import dev.coilix.annotation.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class PriceTable {
  private val rows = AtomicReference(Option.empty[Map[String, BigDecimal]])

  def create(): Unit = rows.set(Some(Map.empty))

  def all: Map[String, BigDecimal] = rows.get.getOrElse(throw IllegalStateException("The price table does not exist yet"))
}
