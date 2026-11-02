package io.github.coilix.common

import scala.reflect.ClassTag

/**
 * Typed access to the runtime class behind a `ClassTag`.
 *
 * `ClassTag.runtimeClass` returns `Class[?]`, so callers would need a cast to get `Class[T]`. These helpers do the cast in one
 * place, which lets APIs take `[T: ClassTag]` instead of an explicit `Class[T]` parameter.
 *
 * ```scala
 * import io.github.coilix.common.ClassTags.runtimeTypeOf
 *
 * def beanOf[T: ClassTag](beans: List[AnyRef]): Option[T] =
 *   beans.collectFirst { case bean if runtimeTypeOf[T].isInstance(bean) => runtimeTypeOf[T].cast(bean) }
 * ```
 */
object ClassTags {

  extension [T](tag: ClassTag[T]) {

    /** The runtime class of `T`, typed as `Class[T]`. */
    def runtimeType: Class[T] = tag.runtimeClass.asInstanceOf[Class[T]]
  }

  /** The runtime class of `T`, taken from the `ClassTag` in scope. */
  def runtimeTypeOf[T: ClassTag]: Class[T] = summon[ClassTag[T]].runtimeType
}
