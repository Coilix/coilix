package dev.coilix.context
package failing

import dev.coilix.annotation.Component

/** A bean whose constructor throws. */
@Component
final class BrokenScale {
  throw IllegalStateException("the scale is not calibrated")
}
