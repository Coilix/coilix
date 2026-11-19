package dev.coilix.context

/** One step of a [[dev.coilix.context.ContextPlan]]. */
enum PlanStep {

  /**
   * Registers a ready instance, such as the context itself or a singleton passed to the builder.
   *
   * @param bean
   *   the instance with its name and type
   */
  case Provide(bean: RegisteredBean)

  /**
   * Creates a bean from its definition. Its dependencies are provided or created by earlier steps.
   *
   * @param definition
   *   the factory, dependencies, lifecycle methods and [[dev.coilix.context.BeanResult]] of the bean
   */
  case Create(definition: BeanDefinition)
}
