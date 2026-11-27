package dev.coilix.context

import java.lang.reflect.Constructor

/**
 * Creates a bean by calling `constructor` with the resolved dependencies.
 *
 * An exception thrown by the constructor becomes a [[dev.coilix.context.BeanCreationException]] with that exception as
 * its cause.
 *
 * @param constructor
 *   the constructor chosen by the [[dev.coilix.context.ConstructorSelector]]
 */
case class ConstructorInstanceFactory(constructor: Constructor[?]) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    FactoryCalls.guarded(s"Constructor of ${constructor.getDeclaringClass.getName}")(constructor.newInstance(arguments*))
}
