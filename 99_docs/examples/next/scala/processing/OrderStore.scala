package com.example
package processing

import dev.coilix.annotation.Repository
import java.util.concurrent.atomic.AtomicReference

@Repository
final class OrderStore {
  private val batches = AtomicReference(List.empty[List[String]])

  def saveAll(orders: List[String]): Unit = batches.getAndUpdate(_ :+ orders): Unit

  def saved: List[List[String]] = batches.get
}
