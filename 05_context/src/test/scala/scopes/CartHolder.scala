package dev.coilix.context
package scopes

import dev.coilix.annotation.Component

@Component
final class CartHolder(val cart: ShoppingCart)
