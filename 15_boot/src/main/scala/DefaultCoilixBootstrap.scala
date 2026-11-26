package dev.coilix.boot

import cats.data.EitherT
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
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
import dev.coilix.context.EffectInterpreter
import dev.coilix.context.ModuleLoader
import dev.coilix.context.RegisteredBean
import dev.coilix.context.ResourceT
import java.lang.annotation.Annotation

/**
 * Scans each base package, drops the beans of classes found in more than one package (while `@Bean` methods of one class
 * that return the same type stay separate beans), adds the components of the enabled modules, applies
 * the [[dev.coilix.context.DefinitionProcessor]]s of the modules, leaves out the replaced beans, adds the extra definitions, and
 * builds the context. The names of replaced beans are dropped from [[dev.coilix.context.BeanDefinition.dependsOn]] of the
 * others, because the replacement is a ready instance. The [[dev.coilix.context.EffectInterpreter]] of the enabled module that
 * gives one creates the beans made by effects; two such modules stop the start with a
 * [[dev.coilix.boot.StartupError.ConflictingEffectInterpreters]].
 *
 * When the modules collect annotations, the classes marked with them, found in the base packages or among the module
 * components, become the `collectedComponents` bean made by the [[dev.coilix.context.ComponentCollector]] instead of beans. Each
 * scanned package, each replaced bean and the names of all beans are logged at `INFO` level. Scanning and planning run in
 * `IO.blocking`. The context is not started: its owner runs [[dev.coilix.context.ConfigurableApplicationContext.start]]. This is the default
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
  ): ResourceT[StartupError, ConfigurableApplicationContext] =
    EitherT(Resource.eval(IO.blocking(planned(basePackages, extraDefinitions, singletons, replacements))))
      .flatMap((definitions, interpreter) => builder.create(definitions, singletons, interpreter).leftMap(StartupError.Context(_)))
      .semiflatTap(context => Resource.eval(logF.info(s"Context ready with beans: ${context.beanNames.mkString(", ")}")))

  private def planned(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean],
    replacements:     BeanReplacements
  ): Either[StartupError, (List[BeanDefinition], Option[EffectInterpreter])] = {
    val loaded      = modules.load()
    val annotations = loaded.flatMap(_.collects).distinct
    val collected   = collectedClasses(basePackages, loaded, annotations)
    val processed   = processedBy(loaded)(discovered(basePackages) ++ fromModules(loaded, annotations))
    for {
      discovered  <- kept(processed, replacements)
      beans        = discovered ++ extraDefinitions
      _           <- checkCollected(beans, singletons, collected, annotations)
      interpreter <- effectInterpreter(loaded)
    } yield (beans ++ collectedDefinition(collected, annotations), interpreter)
  }

  private def effectInterpreter(loaded: List[CoilixModule]): Either[StartupError, Option[EffectInterpreter]] =
    loaded.filter(_.effectInterpreter.isDefined) match {
      case Nil           => Right(None)
      case module :: Nil => Right(module.effectInterpreter)
      case modules       => Left(StartupError.ConflictingEffectInterpreters(modules.map(_.name)))
    }

  private def processedBy(loaded: List[CoilixModule])(definitions: List[BeanDefinition]): List[BeanDefinition] =
    loaded.flatMap(_.definitionProcessors).foldLeft(definitions)((current, processor) => current.map(processor.process))

  private def kept(definitions: List[BeanDefinition], replacements: BeanReplacements): Either[StartupError, List[BeanDefinition]] =
    replacements.unmatched(definitions) match {
      case Nil       => Right(withoutReplaced(definitions, definitions.filter(definition => replaced(definition, replacements))))
      case unmatched => Left(StartupError.UnmatchedBeanReplacement(unmatched))
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
    basePackages.flatMap(scan).distinctBy(definition => (definition.beanName, definition.factory))

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
  ): Either[StartupError, Unit] =
    beans.find(bean => collected.contains(bean.beanClass)).map(bean => conflict(bean.beanClass, annotations)).toLeft(()) *>
      beans.traverse_(checkDependencies(beans.map(_.beanType) ++ singletons.map(_.beanType), collected, annotations))

  private def checkDependencies(
    beanTypes:   List[GenericType],
    collected:   List[Class[?]],
    annotations: List[Class[? <: Annotation]]
  )(bean: BeanDefinition): Either[StartupError, Unit] =
    bean.dependencies.collect { case Dependency.Required(beanType) if !beanTypes.exists(beanType.isAssignableFrom) => beanType }
      .flatMap(beanType => collected.find(componentClass => beanType.isAssignableFrom(GenericType.of(componentClass))))
      .headOption
      .map(componentClass => notABean(bean, componentClass, annotations))
      .toLeft(())

  private def conflict(componentClass: Class[?], annotations: List[Class[? <: Annotation]]): StartupError =
    StartupError.CollectedComponentConflict(componentClass, annotationOf(componentClass, annotations))

  private def notABean(bean: BeanDefinition, componentClass: Class[?], annotations: List[Class[? <: Annotation]]): StartupError =
    StartupError.CollectedComponentRequired(bean.beanName, componentClass, annotationOf(componentClass, annotations))

  private def annotationOf(componentClass: Class[?], annotations: List[Class[? <: Annotation]]): String =
    annotations.filter(componentClass.isAnnotationPresent).map(_.getSimpleName).mkString(", @")
}
