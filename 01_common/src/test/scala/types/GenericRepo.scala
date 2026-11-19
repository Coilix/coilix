package dev.coilix.common
package types

class GenericRepo[F[_]] extends Repo[F] {
  override def name: String = "generic"
}
