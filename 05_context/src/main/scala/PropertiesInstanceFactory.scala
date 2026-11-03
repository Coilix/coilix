package dev.coilix.context

import dev.coilix.config.Environment

/**
 * Creates a `@ConfigurationProperties` bean by binding the configuration section at `prefix` to `beanClass`.
 *
 * @param environment
 *   the application configuration
 * @param prefix
 *   the configuration path from the annotation
 * @param beanClass
 *   the annotated class
 */
final case class PropertiesInstanceFactory(environment: Environment, prefix: String, beanClass: Class[?]) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    environment.bind(prefix, beanClass)
}
