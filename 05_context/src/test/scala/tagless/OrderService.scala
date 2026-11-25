package dev.coilix.context
package tagless

/** A service in tagless final style, with the type class instance as a context bound. */
final class OrderService[F[_]: Async](val repo: OrderRepository[F]) {

  /** The `Async` instance the service received. */
  def async: Async[F] = summon[Async[F]]
}
