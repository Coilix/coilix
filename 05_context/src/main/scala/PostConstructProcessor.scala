package dev.coilix.context

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Invokes the `@PostConstruct` methods that return `Unit` from [[dev.coilix.context.BeanDefinition.lifecycle]] and returns the
 * same instance. Methods that return an effect are left to the [[dev.coilix.context.EffectInterpreter]].
 *
 * An exception thrown by a method becomes a [[dev.coilix.context.BeanCreationException]] naming the bean and the method, with the
 * original exception as its cause. For a [[dev.coilix.context.ProxiedBean]] the methods run on the instance behind the proxy,
 * and the proxy is returned. This is the default [[dev.coilix.context.BeanPostProcessor]].
 */
final class PostConstructProcessor extends BeanPostProcessor {

  override def postProcess(definition: BeanDefinition, instance: AnyRef): AnyRef = {
    definition.lifecycle.plainInit.foreach(invoke(definition, ProxiedBean.unwrap(instance)))
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
