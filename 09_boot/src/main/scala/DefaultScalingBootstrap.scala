package org.scaling.boot

import org.scaling.annotation.Slf4s
import org.scaling.context.ApplicationContextBuilder
import org.scaling.context.BeanDefinition
import org.scaling.context.ClassPathScanner
import org.scaling.context.ConfigurableApplicationContext
import org.scaling.context.RegisteredBean

@Slf4s
final class DefaultScalingBootstrap(scanner: ClassPathScanner, builder: ApplicationContextBuilder) extends ScalingBootstrap {

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
