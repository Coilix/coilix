package com.example
package scopes

import dev.coilix.annotation.Service
import dev.coilix.context.Provider

@Service
final class Checkout(carts: Provider[ShoppingCart]) {
  def newCart(): ShoppingCart = carts.get()
}
