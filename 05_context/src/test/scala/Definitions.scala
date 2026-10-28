package org.scaling.context

import org.scaling.config.ComponentProvider

object Definitions {
  def of(classes: Class[?]*): List[BeanDefinition] =
    classes.toList.flatMap(BeanDefinitionFactory.default(using ComponentProvider.builtIn).create)
}
