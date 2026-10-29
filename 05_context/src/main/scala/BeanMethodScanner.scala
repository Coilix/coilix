package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath

trait BeanMethodScanner {
  def scan(owner: Class[?]): List[BeanDefinition]
}

object BeanMethodScanner {
  given ConfigPath[BeanMethodScanner] = ConfigPath[BeanMethodScanner]("coilix.context.bean-method-scanner")

  def default(using components: ComponentProvider): BeanMethodScanner =
    components.get[BeanMethodScanner](ReflectiveBeanMethodScanner(ParameterDependencyMapper.default))
}
