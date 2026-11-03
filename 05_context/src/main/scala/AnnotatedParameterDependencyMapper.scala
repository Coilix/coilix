package dev.coilix.context

import dev.coilix.annotation.Value
import dev.coilix.config.ValueExpression
import java.lang.reflect.Parameter

/**
 * Maps a parameter annotated with `@Value` to a [[dev.coilix.context.Dependency.Value]], and any other parameter by its type with
 * the
 * [[dev.coilix.context.DependencyTypeMapper]].
 *
 * This is the default [[dev.coilix.context.ParameterDependencyMapper]].
 */
final class AnnotatedParameterDependencyMapper(typeMapper: DependencyTypeMapper) extends ParameterDependencyMapper {

  override def map(parameter: Parameter): Dependency =
    Option(parameter.getAnnotation(classOf[Value]))
      .map(value => Dependency.Value(ValueExpression.parse(value.value), parameter.getParameterizedType))
      .getOrElse(typeMapper.map(parameter.getParameterizedType))
}
