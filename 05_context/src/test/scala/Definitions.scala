package io.github.coilix.context

import io.github.coilix.config.ComponentProvider

object Definitions {
  def of(classes: Class[?]*): List[BeanDefinition] =
    classes.toList.flatMap(BeanDefinitionFactory.default(using ComponentProvider.builtIn).create)
}
