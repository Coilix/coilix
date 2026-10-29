package io.github.coilix.boot

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath
import io.github.coilix.context.ApplicationContextBuilder
import io.github.coilix.context.BeanDefinition
import io.github.coilix.context.ClassPathScanner
import io.github.coilix.context.ConfigurableApplicationContext
import io.github.coilix.context.RegisteredBean

trait CoilixBootstrap {
  def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean]
  ): ConfigurableApplicationContext
}

object CoilixBootstrap {
  given ConfigPath[CoilixBootstrap] = ConfigPath[CoilixBootstrap]("coilix.boot.bootstrap")

  def default(using components: ComponentProvider): CoilixBootstrap =
    components.get[CoilixBootstrap](DefaultCoilixBootstrap(ClassPathScanner.default, ApplicationContextBuilder.default))
}
