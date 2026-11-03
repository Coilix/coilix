package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

trait ClassPathScanner {
  def scan(basePackage: String): List[BeanDefinition]
}

object ClassPathScanner {
  given ConfigPath[ClassPathScanner] = ConfigPath[ClassPathScanner]("coilix.context.class-path-scanner")

  def default(using components: ComponentProvider): ClassPathScanner =
    components.get[ClassPathScanner](ReflectionsClassPathScanner(BeanDefinitionFactory.default))
}
