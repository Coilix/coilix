package io.github.coilix.context

import io.github.coilix.config.Environment

final case class PropertiesInstanceFactory(environment: Environment, prefix: String, beanClass: Class[?]) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    environment.bind(prefix, beanClass)
}
