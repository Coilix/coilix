package dev.coilix.context

/**
 * Names a bean after its class with the first letter in lower case, so `OrderService` becomes `orderService`. Classes
 * without a simple name, such as anonymous classes, use the fully qualified name.
 *
 * This is the default [[dev.coilix.context.BeanNameGenerator]].
 */
final class SimpleBeanNameGenerator extends BeanNameGenerator {

  override def generate(beanClass: Class[?]): String =
    beanClass.getSimpleName match {
      case ""         => beanClass.getName
      case simpleName => simpleName.head.toLower +: simpleName.tail
    }
}
