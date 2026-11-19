package dev.coilix.context
package effects

final case class Scoped[F[_], A](value: A)
