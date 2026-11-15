package dev.coilix.context

import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Finds methods annotated with `@PostConstruct` and `@PreDestroy` in the bean class and its superclasses.
 *
 * Private methods are included and made accessible. A method overridden in a subclass counts once. Methods of each kind are
 * ordered by name. This is the default [[dev.coilix.context.LifecycleMethodScanner]].
 */
final class AnnotatedLifecycleMethodScanner extends LifecycleMethodScanner {

  override def scan(beanName: String, beanClass: Class[?]): LifecycleMethods =
    LifecycleMethods(
      init    = annotated(beanName, beanClass, classOf[PostConstruct]),
      destroy = annotated(beanName, beanClass, classOf[PreDestroy])
    )

  private def annotated(beanName: String, beanClass: Class[?], annotation: Class[? <: Annotation]): List[Method] =
    hierarchy(beanClass)
      .flatMap(_.getDeclaredMethods.toList)
      .filter(method => !method.isBridge && !method.isSynthetic && method.isAnnotationPresent(annotation))
      .distinctBy(_.getName)
      .sortBy(_.getName)
      .map(validated(beanName, annotation))

  private def hierarchy(beanClass: Class[?]): List[Class[?]] =
    List.unfold(Option[Class[?]](beanClass))(_.map(current => (current, Option(current.getSuperclass))))

  private def validated(beanName: String, annotation: Class[? <: Annotation])(method: Method): Method =
    problemOf(method)
      .map(problem =>
        throw InvalidLifecycleMethodException(s"@${annotation.getSimpleName} method ${method.getName} of bean '$beanName' $problem")
      )
      .getOrElse(method)

  private def problemOf(method: Method): Option[String] =
    List(
      Modifier.isStatic(method.getModifiers) -> "must not be static",
      (method.getParameterCount > 0)         -> "must not take parameters",
      (method.getReturnType != Void.TYPE)    -> "must return Unit",
      !method.trySetAccessible()             ->
        s"cannot be made accessible, open the package ${method.getDeclaringClass.getPackageName} to Coilix"
    ).collectFirst { case (true, problem) => problem }
}
