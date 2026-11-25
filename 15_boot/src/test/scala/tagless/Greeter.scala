package dev.coilix.boot
package tagless

import dev.coilix.annotation.Component

/** A greeter in tagless final style. */
@Component
final class Greeter[F[_]](val delay: Delay[F])
