package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * Binds the type parameter of a component written in tagless final style to the effect of the application, such as
 * `cats.effect.IO`.
 *
 * A component class or `@Bean` method may leave one type parameter unbound when it appears in the types of its dependencies,
 * as `F` in `final class OrderService[F[_]: Async](repo: OrderRepository[F])`. The binding replaces it with `effect` in the
 * type of the bean and of every dependency, so the bean above is `OrderService[IO]` and depends on `OrderRepository[IO]` and
 * `Async[IO]`. The JVM does not keep the kind of a type parameter, so any such parameter is bound, and a definition with
 * several of them is rejected.
 *
 * @param effect
 *   the type constructor of the effect, given by the module named in [[dev.coilix.context.CoilixModule.effectType]]
 */
case class EffectTypeBinding(effect: Class[?]) {

  /**
   * The definition with its type parameter bound to the effect, or unchanged when it has none.
   *
   * @throws dev.coilix.context.UnsupportedDependencyTypeException
   *   if the definition has more than one unbound type parameter
   */
  def bind(definition: BeanDefinition): BeanDefinition =
    EffectTypeBinding.variablesOf(definition) match {
      case Nil             => definition
      case variable :: Nil => bound(definition, Map(variable -> GenericType.Concrete(effect, Nil)))
      case variables       => throw several(definition, variables)
    }

  private def bound(definition: BeanDefinition, bindings: Map[String, GenericType]): BeanDefinition =
    definition.copy(
      beanType     = definition.beanType.substitute(bindings),
      dependencies = definition.dependencies.map(dependency => EffectTypeBinding.substituted(dependency, bindings))
    )

  private def several(definition: BeanDefinition, variables: List[String]): UnsupportedDependencyTypeException =
    UnsupportedDependencyTypeException(
      s"Bean ${definition.beanName} (${definition.beanClass.getName}) leaves the type parameters ${variables.mkString(", ")} " +
        s"unbound, but only one, the effect, can be bound to ${effect.getName}. Declare the bean through a @Bean method with " +
        "concrete types instead"
    )
}

/** Reading and checking the unbound type parameters of bean definitions. */
object EffectTypeBinding {

  /** The type variables left in the type of the bean or of its dependencies, in order of appearance. */
  def variablesOf(definition: BeanDefinition): List[String] =
    (definition.beanType.variables ++ variablesIn(definition.dependencies)).distinct

  /** The type variables in the types of `dependencies`, in order of appearance. */
  def variablesIn(dependencies: List[Dependency]): List[String] = dependencies.flatMap(typeOf).flatMap(_.variables).distinct

  /**
   * Checks that no definition has an unbound type parameter left.
   *
   * @throws dev.coilix.context.UnsupportedDependencyTypeException
   *   for the first definition that has one, with a hint about the module that binds it
   */
  def requireBound(definitions: List[BeanDefinition]): Unit =
    definitions.find(definition => variablesOf(definition).nonEmpty).foreach(definition => throw unbound(definition))

  private def unbound(definition: BeanDefinition): UnsupportedDependencyTypeException =
    UnsupportedDependencyTypeException(
      s"Bean ${definition.beanName} (${definition.beanClass.getName}) has the type ${definition.beanType} with the unbound type " +
        s"parameter ${variablesOf(definition).mkString(", ")}. Add a module that binds it to the effect of the application, " +
        "such as coilix-concurrency for cats.effect.IO, or declare the bean through a @Bean method with a concrete type"
    )

  private def typeOf(dependency: Dependency): Option[GenericType] =
    dependency match {
      case Dependency.Required(beanType)   => Some(beanType)
      case Dependency.Named(_, beanType)   => Some(beanType)
      case Dependency.Optional(beanType)   => Some(beanType)
      case Dependency.Deferred(beanType)   => Some(beanType)
      case Dependency.Collection(beanType) => Some(beanType)
      case Dependency.Value(_, _)          => None
    }

  private def substituted(dependency: Dependency, bindings: Map[String, GenericType]): Dependency =
    dependency match {
      case Dependency.Required(beanType)    => Dependency.Required(beanType.substitute(bindings))
      case Dependency.Named(name, beanType) => Dependency.Named(name, beanType.substitute(bindings))
      case Dependency.Optional(beanType)    => Dependency.Optional(beanType.substitute(bindings))
      case Dependency.Deferred(beanType)    => Dependency.Deferred(beanType.substitute(bindings))
      case Dependency.Collection(beanType)  => Dependency.Collection(beanType.substitute(bindings))
      case value @ Dependency.Value(_, _)   => value
    }
}
