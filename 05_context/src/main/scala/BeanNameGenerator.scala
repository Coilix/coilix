package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath

trait BeanNameGenerator {
  def generate(beanClass: Class[?]): String
}

object BeanNameGenerator {
  given ConfigPath[BeanNameGenerator] = ConfigPath[BeanNameGenerator]("coilix.context.bean-name-generator")

  def default(using components: ComponentProvider): BeanNameGenerator =
    components.get[BeanNameGenerator](SimpleBeanNameGenerator())
}
