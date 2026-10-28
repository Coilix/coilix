package org.scaling.boot

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath
import org.scaling.context.ApplicationContextBuilder
import org.scaling.context.BeanDefinition
import org.scaling.context.ClassPathScanner
import org.scaling.context.ConfigurableApplicationContext
import org.scaling.context.RegisteredBean

trait ScalingBootstrap {
  def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean]
  ): ConfigurableApplicationContext
}

object ScalingBootstrap {
  given ConfigPath[ScalingBootstrap] = ConfigPath[ScalingBootstrap]("scaling.boot.bootstrap")

  def default(using components: ComponentProvider): ScalingBootstrap =
    components.get[ScalingBootstrap](DefaultScalingBootstrap(ClassPathScanner.default, ApplicationContextBuilder.default))
}
