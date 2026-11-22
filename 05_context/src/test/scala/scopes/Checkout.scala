package dev.coilix.context
package scopes

import dev.coilix.annotation.Component

@Component
final class Checkout(carts: Provider[ShoppingCart]) {
  def start(): ShoppingCart = carts.get()
}
