package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

trait BeanMethodScanner {
  def scan(owner: Class[?]): List[BeanDefinition]
}

object BeanMethodScanner {
  given ConfigPath[BeanMethodScanner] = ConfigPath[BeanMethodScanner]("coilix.context.bean-method-scanner")

  def default(using components: ComponentProvider): BeanMethodScanner =
    components.get[BeanMethodScanner](ReflectiveBeanMethodScanner(ParameterDependencyMapper.default))
}
