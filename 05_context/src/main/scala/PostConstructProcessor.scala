package dev.coilix.context

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Invokes the `@PostConstruct` methods from [[dev.coilix.context.BeanDefinition.lifecycle]] and returns the same instance.
 *
 * An exception thrown by a method becomes a [[dev.coilix.context.BeanCreationException]] naming the bean and the method, with the
 * original exception as its cause. This is the default [[dev.coilix.context.BeanPostProcessor]].
 */
final class PostConstructProcessor extends BeanPostProcessor {

  override def postProcess(definition: BeanDefinition, instance: AnyRef): AnyRef = {
    definition.lifecycle.init.foreach(invoke(definition, instance))
    instance
  }

  private def invoke(definition: BeanDefinition, instance: AnyRef)(method: Method): Unit =
    try {
      val _ = method.invoke(instance)
    } catch {
      case failure: InvocationTargetException => throw failed(definition, method, failure.getCause)
    }

  private def failed(definition: BeanDefinition, method: Method, cause: Throwable): BeanCreationException =
    BeanCreationException(s"@PostConstruct method ${method.getName} of bean '${definition.beanName}' failed: $cause", Some(cause))
}
