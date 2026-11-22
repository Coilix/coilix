package dev.coilix.context

import dev.coilix.annotation.ConditionalOnClass
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.ConditionalOnProperty
import dev.coilix.annotation.Profile
import java.lang.reflect.AnnotatedElement

/**
 * Reads [[dev.coilix.annotation.Profile]], [[dev.coilix.annotation.ConditionalOnProperty]],
 * [[dev.coilix.annotation.ConditionalOnClass]] and [[dev.coilix.annotation.ConditionalOnMissingBean]].
 *
 * For a `@Bean` method, the conditions of its configuration class come first and the conditions of the method are added. This
 * is the default [[dev.coilix.context.ConditionScanner]], selected when `coilix.context.condition-scanner` is not set.
 *
 * @param classLoader
 *   the class loader that checks [[dev.coilix.annotation.ConditionalOnClass]]
 */
final class AnnotatedConditionScanner(classLoader: ClassLoader) extends ConditionScanner {

  override def scan(definition: BeanDefinition): BeanConditions =
    sources(definition).map(conditionsOf).foldLeft(BeanConditions.none)(combined)

  private def sources(definition: BeanDefinition): List[AnnotatedElement] =
    definition.factory match {
      case MethodInstanceFactory(method) => List(method.getDeclaringClass, method)
      case _                             => List(definition.beanClass)
    }

  private def conditionsOf(element: AnnotatedElement): BeanConditions =
    BeanConditions(
      List(profile(element), property(element), classes(element)).flatten,
      element.isAnnotationPresent(classOf[ConditionalOnMissingBean]),
      Option(element.getAnnotation(classOf[ConditionalOnMissingBean])).toList.flatMap(_.value.toList)
    )

  private def profile(element: AnnotatedElement): Option[BeanCondition] =
    Option(element.getAnnotation(classOf[Profile])).map(annotation => ProfileCondition(annotation.value.toList))

  private def property(element: AnnotatedElement): Option[BeanCondition] =
    Option(element.getAnnotation(classOf[ConditionalOnProperty]))
      .map(annotation => PropertyCondition(annotation.name, annotation.havingValue, annotation.matchIfMissing))

  private def classes(element: AnnotatedElement): Option[BeanCondition] =
    Option(element.getAnnotation(classOf[ConditionalOnClass]))
      .map(annotation => ClassCondition(annotation.value.toList, classLoader))

  private def combined(left: BeanConditions, right: BeanConditions): BeanConditions =
    BeanConditions(
      left.conditions   ++ right.conditions,
      left.onlyIfMissing || right.onlyIfMissing,
      left.missingTypes ++ right.missingTypes
    )
}
