package dev.coilix.boot

import dev.coilix.annotation.Slf4s
import dev.coilix.common.GenericType
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.CoilixModule
import dev.coilix.context.ComponentCollector
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.Dependency
import dev.coilix.context.ModuleLoader
import dev.coilix.context.NoSuchBeanException
import dev.coilix.context.RegisteredBean
import java.lang.annotation.Annotation

/**
 * Scans each base package, drops classes found in more than one package, adds the components of the enabled modules, applies
 * the [[dev.coilix.context.DefinitionProcessor]]s of the modules, leaves out the replaced beans, adds the extra definitions, and
 * builds the context. The names of replaced beans are dropped from [[dev.coilix.context.BeanDefinition.dependsOn]] of the
 * others, because the replacement is a ready instance.
 *
 * When the modules collect annotations, the classes marked with them, found in the base packages or among the module
 * components, become the `collectedComponents` bean made by the [[dev.coilix.context.ComponentCollector]] instead of beans. Each
 * scanned package, each replaced bean and the names of all beans are logged at `INFO` level. This is the default
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
 * @param collector
 *   creates the definition of the `collectedComponents` bean
 */
@Slf4s
final class DefaultCoilixBootstrap(
  scanner:   ClassPathScanner,
  builder:   ApplicationContextBuilder,
  modules:   ModuleLoader,
  factory:   BeanDefinitionFactory,
  collector: ComponentCollector
) extends CoilixBootstrap {

  override def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean],
    replacements:     BeanReplacements
  ): ConfigurableApplicationContext = {
    val loaded      = modules.load()
    val annotations = loaded.flatMap(_.collects).distinct
    val collected   = collectedClasses(basePackages, loaded, annotations)
    val processed   = processedBy(loaded)(discovered(basePackages) ++ fromModules(loaded, annotations))
    val beans       = kept(processed, replacements) ++ extraDefinitions
    checkCollected(beans, singletons, collected, annotations)
    ready(builder.build(beans ++ collectedDefinition(collected, annotations), singletons))
  }

  private def processedBy(loaded: List[CoilixModule])(definitions: List[BeanDefinition]): List[BeanDefinition] =
    loaded.flatMap(_.definitionProcessors).foldLeft(definitions)((current, processor) => current.map(processor.process))

  private def kept(definitions: List[BeanDefinition], replacements: BeanReplacements): List[BeanDefinition] =
    replacements.unmatched(definitions) match {
      case Nil       => withoutReplaced(definitions, definitions.filter(definition => replaced(definition, replacements)))
      case unmatched => throw UnmatchedBeanReplacementException(unmatched)
    }

  private def withoutReplaced(definitions: List[BeanDefinition], replaced: List[BeanDefinition]): List[BeanDefinition] = {
    val replacedNames = replaced.map(_.beanName)
    definitions
      .filterNot(replaced.contains)
      .map(definition => definition.copy(dependsOn = definition.dependsOn.filterNot(replacedNames.contains)))
  }

  private def replaced(definition: BeanDefinition, replacements: BeanReplacements): Boolean = {
    val isReplaced = replacements.replaces(definition)
    if (isReplaced) log.info(s"Replacing bean ${definition.beanName} (${definition.beanClass.getName})")
    isReplaced
  }

  private def discovered(basePackages: List[String]): List[BeanDefinition] =
    basePackages.flatMap(scan).distinctBy(_.beanClass)

  private def fromModules(loaded: List[CoilixModule], annotations: List[Class[? <: Annotation]]): List[BeanDefinition] =
    loaded.flatMap(_.components).filterNot(isCollected(annotations)).flatMap(factory.create)

  private def scan(basePackage: String): List[BeanDefinition] = {
    log.info(s"Scanning package $basePackage")
    scanner.scan(basePackage)
  }

  private def collectedClasses(
    basePackages: List[String],
    loaded:       List[CoilixModule],
    annotations:  List[Class[? <: Annotation]]
  ): List[Class[?]] =
    (basePackages.flatMap(scanner.scanAnnotated(_, annotations)) ++
      loaded.flatMap(_.components).filter(isCollected(annotations))).distinct

  private def isCollected(annotations: List[Class[? <: Annotation]])(componentClass: Class[?]): Boolean =
    annotations.exists(componentClass.isAnnotationPresent)

  private def collectedDefinition(collected: List[Class[?]], annotations: List[Class[? <: Annotation]]): List[BeanDefinition] =
    if (annotations.isEmpty) Nil else List(collector.collect(collected))

  private def checkCollected(
    beans:       List[BeanDefinition],
    singletons:  List[RegisteredBean],
    collected:   List[Class[?]],
    annotations: List[Class[? <: Annotation]]
  ): Unit = {
    beans.find(bean => collected.contains(bean.beanClass)).foreach(bean => throw conflict(bean.beanClass, annotations))
    beans.foreach(checkDependencies(beans.map(_.beanType) ++ singletons.map(_.beanType), collected, annotations))
  }

  private def checkDependencies(
    beanTypes:   List[GenericType],
    collected:   List[Class[?]],
    annotations: List[Class[? <: Annotation]]
  )(bean: BeanDefinition): Unit =
    bean.dependencies.collect { case Dependency.Required(beanType) if !beanTypes.exists(beanType.isAssignableFrom) => beanType }
      .flatMap(beanType => collected.find(componentClass => beanType.isAssignableFrom(GenericType.of(componentClass))))
      .foreach(componentClass => throw notABean(bean, componentClass, annotations))

  private def conflict(componentClass: Class[?], annotations: List[Class[? <: Annotation]]): CollectedComponentConflictException =
    CollectedComponentConflictException(componentClass, annotationOf(componentClass, annotations))

  private def notABean(
    bean:           BeanDefinition,
    componentClass: Class[?],
    annotations:    List[Class[? <: Annotation]]
  ): NoSuchBeanException =
    new NoSuchBeanException(
      s"Bean '${bean.beanName}' requires '${componentClass.getName}', which is marked with " +
        s"@${annotationOf(componentClass, annotations)} and collected by a module, so it is not a bean. " +
        "Inject the module bean that manages it instead"
    )

  private def annotationOf(componentClass: Class[?], annotations: List[Class[? <: Annotation]]): String =
    annotations.filter(componentClass.isAnnotationPresent).map(_.getSimpleName).mkString(", @")

  private def ready(context: ConfigurableApplicationContext): ConfigurableApplicationContext = {
    log.info(s"Context ready with beans: ${context.beanNames.mkString(", ")}")
    context
  }
}
