package dev.coilix.context

import dev.coilix.config.ComponentProvider

object Definitions {
  def of(classes: Class[?]*): List[BeanDefinition] =
    classes.toList.flatMap(BeanDefinitionFactory.default(using ComponentProvider.builtIn).create)
}
