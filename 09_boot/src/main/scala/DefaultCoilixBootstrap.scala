package dev.coilix.boot

import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.ModuleLoader
import dev.coilix.context.RegisteredBean

/**
 * Scans each base package, drops classes found in more than one package, adds the components of the enabled modules, leaves out
 * the replaced beans, adds the extra definitions, and builds the context.
 *
 * Each scanned package, each replaced bean and the names of all beans are logged at `INFO` level. This is the default
 * [[dev.coilix.boot.CoilixBootstrap]].
 *
 * @param scanner
 *   finds components in a package
 * @param builder
 *   creates the beans
 * @param modules
 *   finds the modules on the classpath
 * @param factory
 *   creates the definitions of module components
 */
@Slf4s
final class DefaultCoilixBootstrap(
  scanner: ClassPathScanner,
  builder: ApplicationContextBuilder,
  modules: ModuleLoader,
  factory: BeanDefinitionFactory
) extends CoilixBootstrap {

  override def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean],
    replacements:     BeanReplacements
  ): ConfigurableApplicationContext =
    ready(builder.build(kept(discovered(basePackages) ++ fromModules, replacements) ++ extraDefinitions, singletons))

  private def kept(definitions: List[BeanDefinition], replacements: BeanReplacements): List[BeanDefinition] =
    replacements.unmatched(definitions) match {
      case Nil       => definitions.filterNot(definition => replaced(definition, replacements))
      case unmatched => throw UnmatchedBeanReplacementException(unmatched)
    }

  private def replaced(definition: BeanDefinition, replacements: BeanReplacements): Boolean = {
    val isReplaced = replacements.replaces(definition)
    if (isReplaced) log.info(s"Replacing bean ${definition.beanName} (${definition.beanClass.getName})")
    isReplaced
  }

  private def discovered(basePackages: List[String]): List[BeanDefinition] =
    basePackages.flatMap(scan).distinctBy(_.beanClass)

  private def fromModules: List[BeanDefinition] =
    modules.load().flatMap(_.components).flatMap(factory.create)

  private def scan(basePackage: String): List[BeanDefinition] = {
    log.info(s"Scanning package $basePackage")
    scanner.scan(basePackage)
  }

  private def ready(context: ConfigurableApplicationContext): ConfigurableApplicationContext = {
    log.info(s"Context ready with beans: ${context.beanNames.mkString(", ")}")
    context
  }
}
