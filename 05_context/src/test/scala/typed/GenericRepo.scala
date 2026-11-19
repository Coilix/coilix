package dev.coilix.context
package typed

final class GenericRepo[F[_]](override val name: String) extends Repo[F]
