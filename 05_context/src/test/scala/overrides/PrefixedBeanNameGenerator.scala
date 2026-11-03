package dev.coilix.context
package overrides

import dev.coilix.context.BeanNameGenerator

final class PrefixedBeanNameGenerator(prefix: String, delegate: BeanNameGenerator) extends BeanNameGenerator {
  override def generate(beanClass: Class[?]): String = prefix + delegate.generate(beanClass)
}
