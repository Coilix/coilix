package dev.coilix.context
package ordering

import dev.coilix.annotation.Component

@Component
final class CompositeItem(val parts: List[Item]) extends Item
