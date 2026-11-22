package dev.coilix.events
package model

enum OrderEvent {
  case Placed(id: Int)

  case Cancelled(id: Int)
}
