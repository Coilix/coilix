package org.scaling.boot

import org.scaling.config.EnvironmentLoader
import org.scaling.context.ConfigurableApplicationContext
import scala.reflect.ClassTag

trait ScalingApplicationRunner {
  def run[T: ClassTag](args: List[String]): ConfigurableApplicationContext
}

object ScalingApplicationRunner {
  def default: ScalingApplicationRunner =
    DefaultScalingApplicationRunner(
      SimpleArgumentsParser(),
      EnvironmentLoader.default,
      components => StartupComponents.default(using components)
    )
}
