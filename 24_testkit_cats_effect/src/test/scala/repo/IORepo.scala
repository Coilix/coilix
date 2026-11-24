package dev.coilix.testkit.catseffect
package repo

import cats.effect.IO
import dev.coilix.annotation.Component

@Component
final class IORepo extends Repo[IO] {
  override def find(id: Int): IO[Option[String]] = IO.pure(Option.when(id == 1)("tea"))
}
