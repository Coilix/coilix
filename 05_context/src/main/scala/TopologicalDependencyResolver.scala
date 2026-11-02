package io.github.coilix.context

import scala.annotation.tailrec

final class TopologicalDependencyResolver(selector: CandidateSelector) extends DependencyResolver {

  override def resolve(definitions: List[BeanDefinition], provided: List[Class[?]]): List[BeanDefinition] =
    order(definitions.map(pendingBean(definitions, provided)), Nil)

  @tailrec
  private def order(pending: List[PendingBean], resolved: List[BeanDefinition]): List[BeanDefinition] =
    pending.partition(isReady(resolved)) match {
      case (Nil, Nil)       => resolved
      case (Nil, blocked)   => throw CircularDependencyException(blocked.map(_.definition.beanName))
      case (ready, blocked) => order(blocked, resolved ++ ready.map(_.definition))
    }

  private def isReady(resolved: List[BeanDefinition])(pending: PendingBean): Boolean =
    pending.prerequisites.forall(resolved.contains)

  private def pendingBean(definitions: List[BeanDefinition], provided: List[Class[?]])(definition: BeanDefinition): PendingBean =
    PendingBean(definition, prerequisites(definition, definitions, provided))

  private def prerequisites(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[Class[?]]
  ): List[BeanDefinition] =
    requiredTypes(definition)
      .filterNot(isProvided(provided))
      .map(prerequisite(definition, definitions))
      .distinct

  private def requiredTypes(definition: BeanDefinition): List[Class[?]] =
    definition.dependencies.collect { case Dependency.Required(beanType) => beanType }

  private def isProvided(provided: List[Class[?]])(beanType: Class[?]): Boolean =
    provided.exists(beanType.isAssignableFrom)

  private def prerequisite(definition: BeanDefinition, definitions: List[BeanDefinition])(beanType: Class[?]): BeanDefinition =
    selector.select(beanType, definitions).getOrElse(throw unsatisfied(definition, beanType))

  private def unsatisfied(definition: BeanDefinition, beanType: Class[?]): NoSuchBeanException =
    new NoSuchBeanException(s"No bean satisfies dependency '${beanType.getName}' required by '${definition.beanName}'")
}
