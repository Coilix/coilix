package com.example
package processing

import dev.coilix.annotation.Component
import java.util.concurrent.atomic.AtomicReference

@Component
final class Invoices {
  private val exported = AtomicReference(Set.empty[String])

  def customers: List[String] = List("acme", "globex", "initech", "umbrella", "hooli")

  def send(customer: String): Unit = exported.getAndUpdate(_ + customer): Unit

  def exportedCustomers: Set[String] = exported.get
}
