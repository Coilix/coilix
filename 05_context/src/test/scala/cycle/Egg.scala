package io.github.coilix.context.cycle

import io.github.coilix.annotation.Component

@Component
final class Egg(val chicken: Chicken)
