package dev.coilix.context

import scala.annotation.tailrec

/**
 * Orders bean definitions so that every bean comes after the beans it depends on.
 *
 * A bean waits for:
 *
 *  - the bean chosen by the [[dev.coilix.context.CandidateSelector]] for each required dependency,
 *  - the bean chosen for each `Option[T]` dependency, when there is one,
 *  - every bean that matches a `List[T]` dependency, so the list is complete when the bean is created.
 *
 * A bean never waits for itself through `Option[T]` or `List[T]`, so a bean that implements `T` can collect the other
 * beans of type `T`. Types in `provided` belong to beans that already exist, so nothing waits for them except collections.
 *
 * Beans that are ready in the same round keep the order of `definitions`.
 *
 * @param selector
 *   chooses one bean when several match a required or optional dependency
 */
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
    definition.dependencies.flatMap(prerequisitesOf(definition, definitions, provided)).distinct

  private def prerequisitesOf(definition: BeanDefinition, definitions: List[BeanDefinition], provided: List[Class[?]])(
    dependency: Dependency
  ): List[BeanDefinition] =
    dependency match {
      case Dependency.Required(beanType)   => required(definition, definitions, provided, beanType)
      case Dependency.Optional(beanType)   => optional(definition, definitions, provided, beanType)
      case Dependency.Collection(beanType) => matching(beanType, others(definition, definitions))
      case Dependency.Value(_, _)          => Nil
    }

  private def required(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[Class[?]],
    beanType:    Class[?]
  ): List[BeanDefinition] =
    if (isProvided(provided)(beanType)) Nil
    else List(selector.select(beanType, definitions).getOrElse(throw unsatisfied(definition, beanType)))

  private def optional(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[Class[?]],
    beanType:    Class[?]
  ): List[BeanDefinition] =
    if (isProvided(provided)(beanType)) Nil
    else selector.select(beanType, others(definition, definitions)).toList

  private def matching(beanType: Class[?], definitions: List[BeanDefinition]): List[BeanDefinition] =
    definitions.filter(candidate => beanType.isAssignableFrom(candidate.beanClass))

  private def others(definition: BeanDefinition, definitions: List[BeanDefinition]): List[BeanDefinition] =
    definitions.filterNot(_ eq definition)

  private def isProvided(provided: List[Class[?]])(beanType: Class[?]): Boolean =
    provided.exists(beanType.isAssignableFrom)

  private def unsatisfied(definition: BeanDefinition, beanType: Class[?]): NoSuchBeanException =
    new NoSuchBeanException(s"No bean satisfies dependency '${beanType.getName}' required by '${definition.beanName}'")
}
