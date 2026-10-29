package io.github.coilix.boot

import io.github.coilix.annotation.Slf4s
import io.github.coilix.context.ApplicationContextBuilder
import io.github.coilix.context.BeanDefinition
import io.github.coilix.context.ClassPathScanner
import io.github.coilix.context.ConfigurableApplicationContext
import io.github.coilix.context.RegisteredBean

@Slf4s
final class DefaultCoilixBootstrap(scanner: ClassPathScanner, builder: ApplicationContextBuilder) extends CoilixBootstrap {

  override def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean]
  ): ConfigurableApplicationContext =
    ready(builder.build(discovered(basePackages) ++ extraDefinitions, singletons))

  private def discovered(basePackages: List[String]): List[BeanDefinition] =
    basePackages.flatMap(scan).distinctBy(_.beanClass)

  private def scan(basePackage: String): List[BeanDefinition] = {
    log.info(s"Scanning package $basePackage")
    scanner.scan(basePackage)
  }

  private def ready(context: ConfigurableApplicationContext): ConfigurableApplicationContext = {
    log.info(s"Context ready with beans: ${context.beanNames.mkString(", ")}")
    context
  }
}
