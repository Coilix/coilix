package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Value
import java.util.concurrent.atomic.AtomicInteger

/** The lifts of the workshop, `workshop.lifts` of them (two by default), and how many of them hold a car. */
@Component
final class LiftBoard(@Value("${workshop.lifts:2}") val lifts: Int) {
  private val occupied = AtomicInteger()

  /** Puts a car on a free lift; `false` when every lift is taken. */
  def occupy(): Boolean = occupied.getAndUpdate(taken => if (taken < lifts) taken + 1 else taken) < lifts

  /** Takes a car off a lift. */
  def release(): Unit = {
    val _ = occupied.getAndUpdate(taken => (taken - 1).max(0))
  }

  /** The number of free lifts. */
  def free: Int = lifts - occupied.get
}
