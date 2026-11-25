package dev.coilix.context
package tagless

/** An order repository in memory that needs the `Sync` instance of its effect. */
final class InMemoryOrders[F[_]](val sync: Sync[F]) extends OrderRepository[F]
