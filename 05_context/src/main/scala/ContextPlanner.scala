package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Computes the [[dev.coilix.context.ContextPlan]] that builds a context from bean definitions. */
trait ContextPlanner {

  /**
   * The plan that registers `provided` and creates the definitions that are added to the context, in dependency order.
   *
   * @param definitions
   *   all bean definitions, in the order of scanning
   * @param provided
   *   ready instances registered before any bean is created, such as the context itself
   * @return
   *   the error of the [[dev.coilix.context.DependencyResolver]] when the definitions cannot be ordered, for example a
   *   [[dev.coilix.context.ContextError.MissingBean]] or a [[dev.coilix.context.ContextError.CircularDependency]]
   */
  def plan(definitions: List[BeanDefinition], provided: List[RegisteredBean]): Either[ContextError, ContextPlan]
}

/** Configuration path and default implementation of [[dev.coilix.context.ContextPlanner]]. */
object ContextPlanner {

  /**
   * Replace the component in the configuration under `coilix.context.context-planner`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[ContextPlanner] = ConfigPath[ContextPlanner]("coilix.context.context-planner")

  /**
   * The configured implementation, or a [[dev.coilix.context.DefaultContextPlanner]] built from the default definition filter
   * and dependency resolver when `coilix.context.context-planner` is not set.
   */
  def default(using components: ComponentProvider): ContextPlanner =
    components.get[ContextPlanner](DefaultContextPlanner(DefinitionFilter.default, DependencyResolver.default))
}
