package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath
import java.lang.reflect.Constructor

trait ConstructorSelector {
  def select(beanClass: Class[?]): Constructor[?]
}

object ConstructorSelector {
  given ConfigPath[ConstructorSelector] = ConfigPath[ConstructorSelector]("coilix.context.constructor-selector")

  def default(using components: ComponentProvider): ConstructorSelector =
    components.get[ConstructorSelector](AutowiredConstructorSelector())
}
