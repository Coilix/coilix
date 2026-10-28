package org.scaling.boot

import org.scaling.context.ConfigurableApplicationContext
import scala.reflect.ClassTag

object Scaling {
  def run[T: ClassTag](args: Array[String]): ConfigurableApplicationContext =
    ScalingApplicationRunner.default.run[T](args.toList)
}
