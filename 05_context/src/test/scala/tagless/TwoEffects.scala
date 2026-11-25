package dev.coilix.context
package tagless

/** A component with two type parameters to bind. */
final class TwoEffects[F[_], G[_]](val first: OrderRepository[F], val second: OrderRepository[G])
