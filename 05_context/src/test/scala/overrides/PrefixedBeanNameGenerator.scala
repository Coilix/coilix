package io.github.coilix.context.overrides

import io.github.coilix.context.BeanNameGenerator

final class PrefixedBeanNameGenerator(prefix: String, delegate: BeanNameGenerator) extends BeanNameGenerator {
  override def generate(beanClass: Class[?]): String = prefix + delegate.generate(beanClass)
}
