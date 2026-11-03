package dev.coilix.context

import dev.coilix.config.ValueExpression
import java.lang.reflect.Type

/** A constructor or `@Bean` method parameter, classified by how its value is obtained. */
enum Dependency {

  /** A parameter of a bean type. The bean must exist. */
  case Required(beanType: Class[?])

  /** An `Option[T]` parameter: the bean of type `T` when there is one, otherwise `None`. */
  case Optional(beanType: Class[?])

  /** A collection parameter such as `List[T]`: all beans of type `T`, possibly none. */
  case Collection(beanType: Class[?])

  /**
   * A parameter annotated with `@Value`, filled from the configuration.
   *
   * @param expression
   *   the parsed `@Value` expression
   * @param valueType
   *   the parameter type, including type arguments
   */
  case Value(expression: ValueExpression, valueType: Type)
}
