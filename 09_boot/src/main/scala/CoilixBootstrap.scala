package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinition
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean

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
