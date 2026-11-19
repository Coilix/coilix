package dev.coilix.common
package types

trait Repo[F[_]] {
  def name: String
}
