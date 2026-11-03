package dev.coilix.context

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Maps `Option[T]` to [[dev.coilix.context.Dependency.Optional]], any Scala or Java collection of `T` to
 * [[dev.coilix.context.Dependency.Collection]]
 * and every other type to [[dev.coilix.context.Dependency.Required]].
 *
 * This is the default [[dev.coilix.context.DependencyTypeMapper]].
 */
final class GenericDependencyTypeMapper extends DependencyTypeMapper {

  override def map(parameterType: Type): Dependency =
    parameterType match {
      case parameterized: ParameterizedType => mapParameterized(parameterized)
      case beanClass:     Class[?]          => Dependency.Required(beanClass)
      case _ => Dependency.Required(classOf[AnyRef])
    }

  private def mapParameterized(parameterized: ParameterizedType): Dependency =
    mapRaw(rawClassOf(parameterized.getRawType), elementClass(parameterized))

  private def mapRaw(raw: Class[?], element: Class[?]): Dependency =
    if (raw == classOf[Option[?]]) Dependency.Optional(element)
    else if (isCollection(raw)) Dependency.Collection(element)
    else Dependency.Required(raw)

  private def elementClass(parameterized: ParameterizedType): Class[?] =
    parameterized.getActualTypeArguments.headOption.map(rawClassOf).getOrElse(classOf[AnyRef])

  private def rawClassOf(reflected: Type): Class[?] =
    reflected match {
      case cls:           Class[?]          => cls
      case parameterized: ParameterizedType => rawClassOf(parameterized.getRawType)
      case _ => classOf[AnyRef]
    }

  private def isCollection(raw: Class[?]): Boolean =
    classOf[Iterable[?]].isAssignableFrom(raw) || classOf[java.lang.Iterable[?]].isAssignableFrom(raw)
}
