package io.github.coilix.context

final case class PendingBean(definition: BeanDefinition, prerequisites: List[BeanDefinition])
