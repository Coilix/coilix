package io.github.coilix.boot

import io.github.coilix.context.ConfigurableApplicationContext
import scala.reflect.ClassTag

object Coilix {
  def run[T: ClassTag](args: Array[String]): ConfigurableApplicationContext =
    CoilixApplicationRunner.default.run[T](args.toList)
}
