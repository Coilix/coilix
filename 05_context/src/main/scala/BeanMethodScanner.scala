package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait BeanMethodScanner {
  def scan(owner: Class[?]): List[BeanDefinition]
}

object BeanMethodScanner {
  given ConfigPath[BeanMethodScanner] = ConfigPath[BeanMethodScanner]("scaling.context.bean-method-scanner")

  def default(using components: ComponentProvider): BeanMethodScanner =
    components.get[BeanMethodScanner](ReflectiveBeanMethodScanner(ParameterDependencyMapper.default))
}
