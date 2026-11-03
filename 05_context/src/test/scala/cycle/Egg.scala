package dev.coilix.context
package cycle

import dev.coilix.annotation.Component

@Component
final class Egg(val chicken: Chicken)
