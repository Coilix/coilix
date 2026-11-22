package com.example
package scopes

import dev.coilix.annotation.Component
import dev.coilix.annotation.Prototype
import java.util.concurrent.atomic.AtomicReference

@Component
@Prototype
final class ShoppingCart {
  private val items = AtomicReference(List.empty[String])

  def add(item: String): Unit = {
    val _ = items.updateAndGet(_ :+ item)
  }

  def contents: List[String] = items.get
}
