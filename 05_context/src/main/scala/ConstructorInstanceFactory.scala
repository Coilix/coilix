package org.scaling.context

import java.lang.reflect.Constructor

final case class ConstructorInstanceFactory(constructor: Constructor[?]) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    constructor.newInstance(arguments*)
}
