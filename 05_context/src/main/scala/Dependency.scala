package org.scaling.context

import java.lang.reflect.Type
import org.scaling.config.ValueExpression

enum Dependency {
  case Required(beanType: Class[?])
  case Optional(beanType: Class[?])
  case Collection(beanType: Class[?])
  case Value(expression: ValueExpression, valueType: Type)
}
