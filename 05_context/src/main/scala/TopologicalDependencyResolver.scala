package dev.coilix.context

import dev.coilix.common.GenericType
import scala.annotation.tailrec

/**
 * Orders bean definitions so that every bean comes after the beans it depends on.
 *
 * A bean waits for:
 *
 *  - the bean chosen by the [[dev.coilix.context.CandidateSelector]] for each required dependency,
 *  - the bean with the given name for each `@Qualifier` dependency,
 *  - the bean chosen for each `Option[T]` dependency, when there is one,
 *  - every bean that matches a `List[T]` dependency, so the list is complete when the bean is created,
 *  - the bean with each name in [[dev.coilix.context.BeanDefinition.dependsOn]].
 *
 * A name in `dependsOn` that matches no definition stops the resolution with a [[dev.coilix.context.NoSuchBeanException]], and
 * a name of a prototype with a [[dev.coilix.context.BeanCreationException]].
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

  override def resolve(definitions: List[BeanDefinition], provided: List[GenericType]): List[BeanDefinition] =
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

  private def pendingBean(definitions: List[BeanDefinition], provided: List[GenericType])(definition: BeanDefinition): PendingBean =
    PendingBean(definition, prerequisites(definition, definitions, provided))

  private def prerequisites(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType]
  ): List[BeanDefinition] =
    (definition.dependencies.flatMap(prerequisitesOf(definition, definitions, provided)) ++
      definition.dependsOn.map(dependedOn(definition, definitions))).distinct

  private def dependedOn(definition: BeanDefinition, definitions: List[BeanDefinition])(beanName: String): BeanDefinition =
    definitions.find(_.beanName == beanName) match {
      case Some(found) if found.creation == BeanCreation.Prototype => throw dependsOnPrototype(definition, beanName)
      case Some(found)                                             => found
      case None                                                    =>
        throw new NoSuchBeanException(s"No bean named '$beanName' that '${definition.beanName}' depends on")
    }

  private def dependsOnPrototype(definition: BeanDefinition, beanName: String): BeanCreationException =
    new BeanCreationException(
      s"Bean '${definition.beanName}' depends on the prototype '$beanName', which has no single instance to create first"
    )

  private def prerequisitesOf(definition: BeanDefinition, definitions: List[BeanDefinition], provided: List[GenericType])(
    dependency: Dependency
  ): List[BeanDefinition] =
    dependency match {
      case Dependency.Required(beanType)    => required(definition, definitions, provided, beanType)
      case Dependency.Named(name, beanType) => named(definition, definitions, provided, name, beanType)
      case Dependency.Optional(beanType)    => optional(definition, definitions, provided, beanType)
      case Dependency.Deferred(beanType)    => deferred(definition, definitions, provided, beanType)
      case Dependency.Collection(beanType)  => matching(beanType, others(definition, definitions))
      case Dependency.Value(_, _)           => Nil
    }

  private def required(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType],
    beanType:    GenericType
  ): List[BeanDefinition] =
    if (isProvided(provided)(beanType)) Nil
    else List(selector.select(beanType, definitions).getOrElse(throw unsatisfied(definition, beanType)))

  private def named(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType],
    beanName:    String,
    beanType:    GenericType
  ): List[BeanDefinition] =
    definitions.filter(candidate => candidate.beanName == beanName && beanType.isAssignableFrom(candidate.beanType)) match {
      case Nil if isProvided(provided)(beanType) => Nil
      case Nil                                   =>
        throw new NoSuchBeanException(s"No bean named '$beanName' of type '$beanType' required by '${definition.beanName}'")
      case found                                 => found
    }

  private def optional(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType],
    beanType:    GenericType
  ): List[BeanDefinition] =
    if (isProvided(provided)(beanType)) Nil
    else selector.select(beanType, others(definition, definitions)).toList

  private def deferred(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType],
    beanType:    GenericType
  ): List[BeanDefinition] = {
    val _ = required(definition, definitions, provided, beanType)
    Nil
  }

  private def matching(beanType: GenericType, definitions: List[BeanDefinition]): List[BeanDefinition] =
    definitions.filter(candidate => beanType.isAssignableFrom(candidate.beanType))

  private def others(definition: BeanDefinition, definitions: List[BeanDefinition]): List[BeanDefinition] =
    definitions.filterNot(_ eq definition)

  private def isProvided(provided: List[GenericType])(beanType: GenericType): Boolean =
    provided.exists(beanType.isAssignableFrom)

  private def unsatisfied(definition: BeanDefinition, beanType: GenericType): NoSuchBeanException =
    new NoSuchBeanException(s"No bean satisfies dependency '$beanType' required by '${definition.beanName}'")
}
