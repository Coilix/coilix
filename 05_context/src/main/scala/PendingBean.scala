package dev.coilix.context

final case class PendingBean(definition: BeanDefinition, prerequisites: List[BeanDefinition])
