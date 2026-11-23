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
   * @param level
   *   the dependency level of the bean: it depends only on provided beans and on beans of lower levels, so beans of one level
   *   can be created at the same time
   */
  case Create(definition: BeanDefinition, level: Int = 0)
}
