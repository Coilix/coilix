package dev.coilix.context

/**
 * Plans a context: registers the ready instances, leaves out the definitions rejected by the
 * [[dev.coilix.context.DefinitionFilter]] and orders the rest with the [[dev.coilix.context.DependencyResolver]].
 *
 * This is the default [[dev.coilix.context.ContextPlanner]].
 *
 * @param filter
 *   leaves out definitions whose conditions do not hold
 * @param resolver
 *   orders bean definitions by their dependencies
 */
final class DefaultContextPlanner(filter: DefinitionFilter, resolver: DependencyResolver) extends ContextPlanner {

  override def plan(definitions: List[BeanDefinition], provided: List[RegisteredBean]): ContextPlan = {
    val kept    = filter.filter(definitions, provided.map(_.beanClass))
    val ordered = resolver.resolve(kept, provided.map(_.beanType))
    ContextPlan(provided.map(PlanStep.Provide(_)) ++ ordered.map(PlanStep.Create(_)))
  }
}
