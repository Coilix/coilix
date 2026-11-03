package dev.coilix.context

import dev.coilix.annotation.Value
import dev.coilix.config.ValueExpression
import java.lang.reflect.Parameter

final class AnnotatedParameterDependencyMapper(typeMapper: DependencyTypeMapper) extends ParameterDependencyMapper {

  override def map(parameter: Parameter): Dependency =
    Option(parameter.getAnnotation(classOf[Value]))
      .map(value => Dependency.Value(ValueExpression.parse(value.value), parameter.getParameterizedType))
      .getOrElse(typeMapper.map(parameter.getParameterizedType))
}
