package dev.coilix.boot

import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinition
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean

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
