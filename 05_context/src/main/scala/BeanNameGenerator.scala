package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait BeanNameGenerator {
  def generate(beanClass: Class[?]): String
}

object BeanNameGenerator {
  given ConfigPath[BeanNameGenerator] = ConfigPath[BeanNameGenerator]("scaling.context.bean-name-generator")

  def default(using components: ComponentProvider): BeanNameGenerator =
    components.get[BeanNameGenerator](SimpleBeanNameGenerator())
}
