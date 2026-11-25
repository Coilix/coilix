package dev.coilix.testkit
package effects
package ioapp

import java.util.concurrent.atomic.AtomicReference

object IOAppJournal {

  private val entries = AtomicReference(List.empty[String])

  def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  def all: List[String] = entries.get

  def clear(): Unit = entries.set(Nil)
}
