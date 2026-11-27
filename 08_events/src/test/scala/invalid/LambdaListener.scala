package dev.coilix.events
package invalid

import cats.effect.IO
import dev.coilix.events.model.Customer

object LambdaListener {
  val listener: ApplicationEventListener[Customer] = event => IO.unit
}
