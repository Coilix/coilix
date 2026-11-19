package dev.coilix.context

/**
 * The steps that build a context, in order, computed by a [[dev.coilix.context.ContextPlanner]] before any bean is created.
 *
 * The plan is a value, so different interpreters can run it: the synchronous
 * [[dev.coilix.context.DefaultApplicationContextBuilder]] calls each factory directly, while an effect module can create the
 * beans inside its effect and open their resources.
 *
 * @param steps
 *   the ready instances first, then the beans to create, each after its dependencies
 */
final case class ContextPlan(steps: List[PlanStep]) {

  /** The ready instances, in order of registration. */
  def provided: List[RegisteredBean] =
    steps.collect { case PlanStep.Provide(bean) => bean }

  /** The beans to create, each after its dependencies. */
  def creationOrder: List[BeanDefinition] =
    steps.collect { case PlanStep.Create(definition) => definition }

  /** The created beans in the order they are destroyed, the reverse of creation. */
  def destructionOrder: List[BeanDefinition] =
    creationOrder.reverse

  /** The beans whose factory returns an effect, in creation order. */
  def effectful: List[BeanDefinition] =
    creationOrder.filter(_.result.isEffectful)
}
