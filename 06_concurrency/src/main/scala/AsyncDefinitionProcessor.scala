package dev.coilix.concurrency

import dev.coilix.annotation.Async
import dev.coilix.common.GenericType
import dev.coilix.context.BeanDefinition
import dev.coilix.context.DefinitionProcessor
import dev.coilix.context.Dependency
import java.lang.reflect.Method
import scala.concurrent.Future

/**
 * Turns the definition of a bean with [[dev.coilix.annotation.Async]] methods into a definition of a proxy that runs those
 * methods on the [[dev.coilix.concurrency.TaskExecutor]].
 *
 * The proxy implements every trait of the bean class and is registered under the trait that declares the asynchronous
 * methods, so other beans inject that trait. The definition gets the `TaskExecutor` as an extra dependency. Definitions
 * without `@Async` methods stay as they are. The concurrency module provides this processor.
 */
final class AsyncDefinitionProcessor extends DefinitionProcessor {

  /**
   * The proxy definition for a bean with `@Async` methods.
   *
   * @throws dev.coilix.concurrency.InvalidAsyncMethodException
   *   if an `@Async` method returns neither `Unit` nor `Future`, no trait of the bean declares it, or the methods are declared
   *   in different traits
   */
  override def process(definition: BeanDefinition): BeanDefinition =
    asyncMethodsOf(definition.beanClass) match {
      case Nil     => definition
      case methods => proxied(definition, methods)
    }

  private def asyncMethodsOf(beanClass: Class[?]): List[Method] =
    if (beanClass.isInterface) Nil
    else beanClass.getMethods.toList.filter(_.isAnnotationPresent(classOf[Async])).sortBy(_.getName)

  private def proxied(definition: BeanDefinition, methods: List[Method]): BeanDefinition = {
    methods.foreach(checkResult(definition))
    val interfaces     = interfacesOf(definition.beanClass)
    val service        = serviceTrait(definition, methods, interfaces)
    definition.copy(
      beanType     = definition.beanType.asSuper(service).getOrElse(GenericType.of(service)),
      factory      = AsyncProxyFactory(definition.factory, interfaces, methods.map(signature).toSet),
      dependencies = definition.dependencies :+ Dependency.Required(GenericType.of(classOf[TaskExecutor]))
    )
  }

  private def checkResult(definition: BeanDefinition)(method: Method): Unit =
    if (method.getReturnType != Void.TYPE && !classOf[Future[?]].isAssignableFrom(method.getReturnType)) {
      throw InvalidAsyncMethodException(
        s"${label(definition, method)} must return Unit or Future, not ${method.getReturnType.getName}"
      )
    }

  private def serviceTrait(definition: BeanDefinition, methods: List[Method], interfaces: List[Class[?]]): Class[?] =
    methods.map(method => declaringTrait(definition, method, interfaces)).distinct match {
      case single :: Nil => single
      case several       =>
        throw InvalidAsyncMethodException(
          s"The @Async methods of bean '${definition.beanName}' are declared in several traits (" +
            s"${several.map(_.getName).mkString(", ")}). Declare them in one trait, which other beans inject"
        )
    }

  private def declaringTrait(definition: BeanDefinition, method: Method, interfaces: List[Class[?]]): Class[?] =
    interfaces.find(declares(_, method)).getOrElse {
      throw InvalidAsyncMethodException(
        s"${label(definition, method)} is not declared in a trait of the bean. Coilix runs it asynchronously through a proxy " +
          "of a trait, so declare the method in a trait that the bean extends and inject that trait"
      )
    }

  private def declares(interface: Class[?], method: Method): Boolean =
    interface.getMethods.exists(candidate => signature(candidate) == signature(method))

  private def interfacesOf(beanClass: Class[?]): List[Class[?]] =
    List
      .unfold(Option[Class[?]](beanClass))(_.map(current => (current, Option(current.getSuperclass))))
      .flatMap(current => current.getInterfaces.toList.flatMap(withParents))
      .distinct

  private def withParents(interface: Class[?]): List[Class[?]] =
    interface :: interface.getInterfaces.toList.flatMap(withParents)

  private def signature(method: Method): (String, List[Class[?]]) =
    (method.getName, method.getParameterTypes.toList)

  private def label(definition: BeanDefinition, method: Method): String =
    s"@Async method ${method.getName} of bean '${definition.beanName}'"
}
