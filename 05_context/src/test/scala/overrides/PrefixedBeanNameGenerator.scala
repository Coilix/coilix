package org.scaling.context.overrides

import org.scaling.context.BeanNameGenerator

final class PrefixedBeanNameGenerator(prefix: String, delegate: BeanNameGenerator) extends BeanNameGenerator {
  override def generate(beanClass: Class[?]): String = prefix + delegate.generate(beanClass)
}
