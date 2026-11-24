package dev.coilix.testkit.catseffect
package repo

trait Repo[F[_]] {
  def find(id: Int): F[Option[String]]
}
