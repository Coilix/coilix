package dev.coilix.context

import java.lang.reflect.Constructor

/**
 * Creates a bean by calling `constructor` with the resolved dependencies.
 *
 * @param constructor
 *   the constructor chosen by the [[dev.coilix.context.ConstructorSelector]]
 */
final case class ConstructorInstanceFactory(constructor: Constructor[?]) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    constructor.newInstance(arguments*)
}
