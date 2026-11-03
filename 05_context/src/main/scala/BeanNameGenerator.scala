package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

trait BeanNameGenerator {
  def generate(beanClass: Class[?]): String
}

object BeanNameGenerator {
  given ConfigPath[BeanNameGenerator] = ConfigPath[BeanNameGenerator]("coilix.context.bean-name-generator")

  def default(using components: ComponentProvider): BeanNameGenerator =
    components.get[BeanNameGenerator](SimpleBeanNameGenerator())
}
