package dev.coilix.context.ordering

import dev.coilix.annotation.Component

@Component
final class CompositeItem(val parts: List[Item]) extends Item
