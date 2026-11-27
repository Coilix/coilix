package dev.coilix.config
package valuetype

import dev.coilix.annotation.ValueType
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import scala.util.Try

/**
 * Recognizes value types by the [[dev.coilix.annotation.ValueType]] annotation and finds their constructor and accessor
 * through reflection.
 *
 * The class must be concrete and have exactly one public constructor with one parameter. The inner value is read by the
 * public method with the name of that parameter, which a case class field provides. This is the default resolver, returned
 * by [[dev.coilix.config.valuetype.ValueTypeResolver.default]].
 */
case class ReflectiveValueTypeResolver() extends ValueTypeResolver {

  override def isValueType(candidate: Class[?]): Boolean =
    candidate.isAnnotationPresent(classOf[ValueType])

  override def resolve(valueType: Class[?]): ValueTypeDefinition =
    if (!isValueType(valueType)) throw invalid(valueType, s"is not annotated with @${classOf[ValueType].getSimpleName}")
    else if (Modifier.isAbstract(valueType.getModifiers)) throw invalid(valueType, "must be a concrete class")
    else definition(valueType, constructorOf(valueType))

  private def definition(valueType: Class[?], constructor: Constructor[?]): ValueTypeDefinition =
    ValueTypeDefinition(valueType, constructor, accessorOf(valueType, constructor.getParameters.head.getName))

  private def constructorOf(valueType: Class[?]): Constructor[?] =
    valueType.getConstructors.toList match {
      case constructor :: Nil if constructor.getParameterCount == 1 => constructor
      case _                                                        => throw invalid(valueType, "must have exactly one public constructor with one parameter")
    }

  private def accessorOf(valueType: Class[?], name: String): Method =
    Try(valueType.getMethod(name)).toOption
      .filter(_.getParameterCount == 0)
      .getOrElse(throw invalid(valueType, s"must have a public field or method '$name' that returns the inner value"))

  private def invalid(valueType: Class[?], problem: String): InvalidValueTypeException =
    InvalidValueTypeException(s"Value type ${valueType.getName} $problem")
}
