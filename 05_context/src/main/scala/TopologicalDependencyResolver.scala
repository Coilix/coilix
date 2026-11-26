package dev.coilix.context

import cats.syntax.all.*
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
 * A name in `dependsOn` that matches no definition stops the resolution with a [[dev.coilix.context.ContextError.MissingBean]],
 * and a name of a prototype with a [[dev.coilix.context.ContextError.InvalidDependency]].
 *
 * A bean never waits for itself through `Option[T]` or `List[T]`, so a bean that implements `T` can collect the other
 * beans of type `T`. Types in `provided` belong to beans that already exist, so nothing waits for them except collections.
 *
 * Beans that are ready in the same round keep the order of `definitions` and form one level of
 * [[dev.coilix.context.DependencyResolver.resolveLevels]].
 *
 * @param selector
 *   chooses one bean when several match a required or optional dependency
 */
final class TopologicalDependencyResolver(selector: CandidateSelector) extends DependencyResolver {

  override def resolve(definitions: List[BeanDefinition], provided: List[GenericType]): Either[ContextError, List[BeanDefinition]] =
    resolveLevels(definitions, provided).map(_.flatten)

  override def resolveLevels(
    definitions: List[BeanDefinition],
    provided:    List[GenericType]
  ): Either[ContextError, List[List[BeanDefinition]]] =
    definitions.traverse(pendingBean(definitions, provided)).flatMap(pending => order(pending, Nil, Nil))

  @tailrec
  private def order(
    pending:  List[PendingBean],
    resolved: List[BeanDefinition],
    levels:   List[List[BeanDefinition]]
  ): Either[ContextError, List[List[BeanDefinition]]] =
    pending.partition(isReady(resolved)) match {
      case (Nil, Nil)       => Right(levels)
      case (Nil, blocked)   => Left(ContextError.CircularDependency(blocked.map(_.definition.beanName)))
      case (ready, blocked) => order(blocked, resolved ++ ready.map(_.definition), levels :+ ready.map(_.definition))
    }

  private def isReady(resolved: List[BeanDefinition])(pending: PendingBean): Boolean =
    pending.prerequisites.forall(resolved.contains)

  private def pendingBean(definitions: List[BeanDefinition], provided: List[GenericType])(
    definition: BeanDefinition
  ): Either[ContextError, PendingBean] =
    prerequisites(definition, definitions, provided).map(PendingBean(definition, _))

  private def prerequisites(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType]
  ): Either[ContextError, List[BeanDefinition]] =
    (
      definition.dependencies.flatTraverse(prerequisitesOf(definition, definitions, provided)),
      definition.dependsOn.traverse(dependedOn(definition, definitions))
    ).mapN((dependencies, dependedOn) => (dependencies ++ dependedOn).distinct)

  private def dependedOn(definition: BeanDefinition, definitions: List[BeanDefinition])(
    beanName: String
  ): Either[ContextError, BeanDefinition] =
    definitions.find(_.beanName == beanName) match {
      case Some(found) if found.creation == BeanCreation.Prototype => Left(dependsOnPrototype(definition, beanName))
      case Some(found)                                             => Right(found)
      case None                                                    => Left(missingDependedOn(definition, beanName))
    }

  private def dependsOnPrototype(definition: BeanDefinition, beanName: String): ContextError =
    ContextError.InvalidDependency(
      s"Bean '${definition.beanName}' depends on the prototype '$beanName', which has no single instance to create first"
    )

  private def missingDependedOn(definition: BeanDefinition, beanName: String): ContextError =
    ContextError.MissingBean(s"No bean named '$beanName' that '${definition.beanName}' depends on")

  private def prerequisitesOf(definition: BeanDefinition, definitions: List[BeanDefinition], provided: List[GenericType])(
    dependency: Dependency
  ): Either[ContextError, List[BeanDefinition]] =
    dependency match {
      case Dependency.Required(beanType)    => required(definition, definitions, provided, beanType)
      case Dependency.Named(name, beanType) => named(definition, definitions, provided, name, beanType)
      case Dependency.Optional(beanType)    => optional(definition, definitions, provided, beanType)
      case Dependency.Deferred(beanType)    => required(definition, definitions, provided, beanType).as(Nil)
      case Dependency.Collection(beanType)  => Right(matching(beanType, others(definition, definitions)))
      case Dependency.Value(_, _)           => Right(Nil)
    }

  private def required(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType],
    beanType:    GenericType
  ): Either[ContextError, List[BeanDefinition]] =
    if (isProvided(provided)(beanType)) Right(Nil)
    else selector.choose(beanType, definitions).flatMap(_.toRight(unsatisfied(definition, beanType))).map(List(_))

  private def named(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType],
    beanName:    String,
    beanType:    GenericType
  ): Either[ContextError, List[BeanDefinition]] =
    definitions.filter(candidate => candidate.beanName == beanName && beanType.isAssignableFrom(candidate.beanType)) match {
      case Nil if isProvided(provided)(beanType) => Right(Nil)
      case Nil                                   => Left(missingNamed(definition, beanName, beanType))
      case found                                 => Right(found)
    }

  private def missingNamed(definition: BeanDefinition, beanName: String, beanType: GenericType): ContextError =
    ContextError.MissingBean(s"No bean named '$beanName' of type '$beanType' required by '${definition.beanName}'")

  private def optional(
    definition:  BeanDefinition,
    definitions: List[BeanDefinition],
    provided:    List[GenericType],
    beanType:    GenericType
  ): Either[ContextError, List[BeanDefinition]] =
    if (isProvided(provided)(beanType)) Right(Nil)
    else selector.choose(beanType, others(definition, definitions)).map(_.toList)

  private def matching(beanType: GenericType, definitions: List[BeanDefinition]): List[BeanDefinition] =
    definitions.filter(candidate => beanType.isAssignableFrom(candidate.beanType))

  private def others(definition: BeanDefinition, definitions: List[BeanDefinition]): List[BeanDefinition] =
    definitions.filterNot(_ eq definition)

  private def isProvided(provided: List[GenericType])(beanType: GenericType): Boolean =
    provided.exists(beanType.isAssignableFrom)

  private def unsatisfied(definition: BeanDefinition, beanType: GenericType): ContextError =
    ContextError.MissingBean(s"No bean satisfies dependency '$beanType' required by '${definition.beanName}'")
}
