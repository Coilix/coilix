package org.scaling.config

trait DefaultArguments {
  def find(owner: Class[?], parameterIndex: Int): Option[AnyRef]
}
