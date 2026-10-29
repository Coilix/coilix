package io.github.coilix.context

import io.github.coilix.config.ValueExpression
import java.lang.reflect.Type

enum Dependency {
  case Required(beanType: Class[?])
  case Optional(beanType: Class[?])
  case Collection(beanType: Class[?])
  case Value(expression: ValueExpression, valueType: Type)
}
