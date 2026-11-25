package dev.coilix.context
package tagless

/** A stand-in for a type class such as Async, which extends Sync. */
trait Async[F[_]] extends Sync[F]
