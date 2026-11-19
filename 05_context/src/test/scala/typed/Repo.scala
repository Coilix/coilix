package dev.coilix.context
package typed

trait Repo[F[_]] {
  def name: String
}
