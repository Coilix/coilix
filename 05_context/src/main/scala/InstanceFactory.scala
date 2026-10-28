package org.scaling.context

trait InstanceFactory {
  def create(arguments: List[AnyRef]): AnyRef
}
