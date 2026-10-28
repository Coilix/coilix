package org.scaling.context

import java.lang.reflect.Constructor
import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait ConstructorSelector {
  def select(beanClass: Class[?]): Constructor[?]
}

object ConstructorSelector {
  given ConfigPath[ConstructorSelector] = ConfigPath[ConstructorSelector]("scaling.context.constructor-selector")

  def default(using components: ComponentProvider): ConstructorSelector =
    components.get[ConstructorSelector](AutowiredConstructorSelector())
}
