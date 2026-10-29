package io.github.coilix.context

trait InstanceFactory {
  def create(arguments: List[AnyRef]): AnyRef
}
