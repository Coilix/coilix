package io.github.coilix.config

trait DefaultArguments {
  def find(owner: Class[?], parameterIndex: Int): Option[AnyRef]
}
