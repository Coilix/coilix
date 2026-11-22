package dev.coilix.events
package invalid

import dev.coilix.events.model.Customer

object LambdaListener {
  val listener: ApplicationEventListener[Customer] = event => ()
}
