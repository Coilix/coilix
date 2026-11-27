package dev.coilix.context

import dev.coilix.common.GenericType
import java.lang.reflect.Type

/**
 * Maps `Option[T]` to [[dev.coilix.context.Dependency.Optional]], `Provider[T]` to [[dev.coilix.context.Dependency.Deferred]], any Scala or Java collection of `T` to
 * [[dev.coilix.context.Dependency.Collection]] and every other type to [[dev.coilix.context.Dependency.Required]], keeping the
 * type arguments of `T`, so `Repository[IO]` and `Repository[Option]` are different dependencies.
 *
 * This is the default [[dev.coilix.context.DependencyTypeMapper]].
 */
case class GenericDependencyTypeMapper() extends DependencyTypeMapper {

  override def map(parameterType: Type): Dependency =
    GenericType.of(parameterType) match {
      case GenericType.Concrete(raw, element :: _) if raw == classOf[Option[?]]   => Dependency.Optional(element)
      case GenericType.Concrete(raw, element :: _) if raw == classOf[Provider[?]] => Dependency.Deferred(element)
      case GenericType.Concrete(raw, element :: _) if isCollection(raw)           => Dependency.Collection(element)
      case other                                                                  => Dependency.Required(other)
    }

  private def isCollection(raw: Class[?]): Boolean =
    classOf[Iterable[?]].isAssignableFrom(raw) || classOf[java.lang.Iterable[?]].isAssignableFrom(raw)
}
