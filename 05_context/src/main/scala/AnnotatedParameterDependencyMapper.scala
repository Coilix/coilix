package org.scaling.context

import java.lang.reflect.Parameter
import org.scaling.annotation.Value
import org.scaling.config.ValueExpression

final class AnnotatedParameterDependencyMapper(typeMapper: DependencyTypeMapper) extends ParameterDependencyMapper {

  override def map(parameter: Parameter): Dependency =
    Option(parameter.getAnnotation(classOf[Value]))
      .map(value => Dependency.Value(ValueExpression.parse(value.value), parameter.getParameterizedType))
      .getOrElse(typeMapper.map(parameter.getParameterizedType))
}
