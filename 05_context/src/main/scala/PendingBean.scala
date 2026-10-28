package org.scaling.context

final case class PendingBean(definition: BeanDefinition, prerequisites: List[BeanDefinition])
