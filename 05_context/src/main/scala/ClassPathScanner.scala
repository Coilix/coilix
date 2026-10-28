package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait ClassPathScanner {
  def scan(basePackage: String): List[BeanDefinition]
}

object ClassPathScanner {
  given ConfigPath[ClassPathScanner] = ConfigPath[ClassPathScanner]("scaling.context.class-path-scanner")

  def default(using components: ComponentProvider): ClassPathScanner =
    components.get[ClassPathScanner](ReflectionsClassPathScanner(BeanDefinitionFactory.default))
}
