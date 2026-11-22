package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.config.ValueExpression
import java.lang.reflect.Type

/** A constructor or `@Bean` method parameter, classified by how its value is obtained. */
enum Dependency {

  /** A parameter of a bean type. The bean must exist. */
  case Required(beanType: GenericType)

  /**
   * A parameter of a bean type annotated with `@Qualifier`: the bean with this name, which must exist and have a matching type.
   *
   * @param beanName
   *   the name from the annotation
   * @param beanType
   *   the parameter type
   */
  case Named(beanName: String, beanType: GenericType)

  /** An `Option[T]` parameter: the bean of type `T` when there is one, otherwise `None`. */
  case Optional(beanType: GenericType)

  /**
   * A [[dev.coilix.context.Provider]]`[T]` parameter: a bean of type `T` must exist, but it is looked up only when the provider is
   * used, so it does not have to be created first.
   */
  case Deferred(beanType: GenericType)

  /** A collection parameter such as `List[T]`: all beans of type `T`, possibly none. */
  case Collection(beanType: GenericType)

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
