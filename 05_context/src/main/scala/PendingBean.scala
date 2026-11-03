package dev.coilix.context

/**
 * A bean definition waiting to be ordered by the [[dev.coilix.context.TopologicalDependencyResolver]].
 *
 * @param definition
 *   the waiting definition
 * @param prerequisites
 *   definitions that must be created before it
 */
final case class PendingBean(definition: BeanDefinition, prerequisites: List[BeanDefinition])
