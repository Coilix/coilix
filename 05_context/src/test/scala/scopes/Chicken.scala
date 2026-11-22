package dev.coilix.context
package scopes

import dev.coilix.annotation.Component

@Component
final class Chicken(val egg: Provider[Egg])
