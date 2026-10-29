package io.github.coilix.common

import scala.reflect.ClassTag

object ClassTags {

  extension [T](tag: ClassTag[T]) {
    def runtimeType: Class[T] = tag.runtimeClass.asInstanceOf[Class[T]]
  }

  def runtimeTypeOf[T: ClassTag]: Class[T] = summon[ClassTag[T]].runtimeType
}
