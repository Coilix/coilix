package dev.coilix.context

import cats.data.EitherT
import cats.effect.IO
import cats.effect.Resource

/**
 * A `Resource[IO, A]` whose acquisition can end with a predictable error of type `E` instead of a value, such as a context that
 * cannot be created.
 *
 * `value` gives the plain `Resource[IO, Either[E, A]]`; a caller that cannot handle the error raises it, for example with
 * `valueOrF(error => Resource.raiseError(error.exception))`.
 */
type ResourceT[E, A] = EitherT[[x] =>> Resource[IO, x], E, A]
