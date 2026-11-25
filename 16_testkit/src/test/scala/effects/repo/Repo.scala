package dev.coilix.testkit
package effects
package repo

trait Repo[F[_]] {
  def find(id: Int): F[Option[String]]
}
