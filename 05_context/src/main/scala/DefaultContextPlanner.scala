package dev.coilix.context

/**
 * Plans a context: registers the ready instances, leaves out the definitions rejected by the
 * [[dev.coilix.context.DefinitionFilter]] and orders the rest with the [[dev.coilix.context.DependencyResolver]]. A name in
 * [[dev.coilix.context.BeanDefinition.dependsOn]] that belongs to a ready instance is dropped, because that bean exists already.
 *
 * This is the default [[dev.coilix.context.ContextPlanner]].
 *
 * @param filter
 *   leaves out definitions whose conditions do not hold
 * @param resolver
 *   orders bean definitions by their dependencies
 */
case class DefaultContextPlanner(filter: DefinitionFilter, resolver: DependencyResolver) extends ContextPlanner {

  override def plan(definitions: List[BeanDefinition], provided: List[RegisteredBean]): Either[ContextError, ContextPlan] = {
    val kept = filter.filter(definitions, provided.map(_.beanClass)).map(withoutProvidedNames(provided.map(_.beanName)))
    resolver.resolveLevels(kept, provided.map(_.beanType)).map(levels => planOf(provided, levels))
  }

  private def planOf(provided: List[RegisteredBean], levels: List[List[BeanDefinition]]): ContextPlan =
    ContextPlan(provided.map(PlanStep.Provide(_)) ++ levels.zipWithIndex.flatMap((level, index) => created(level, index)))

  private def created(level: List[BeanDefinition], index: Int): List[PlanStep] =
    level.map(PlanStep.Create(_, index))

  private def withoutProvidedNames(providedNames: List[String])(definition: BeanDefinition): BeanDefinition =
    definition.copy(dependsOn = definition.dependsOn.filterNot(providedNames.contains))
}
