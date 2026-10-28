package org.scaling.context

final class SimpleBeanNameGenerator extends BeanNameGenerator {

  override def generate(beanClass: Class[?]): String =
    beanClass.getSimpleName match {
      case ""         => beanClass.getName
      case simpleName => simpleName.head.toLower +: simpleName.tail
    }
}
