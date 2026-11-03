package dev.coilix.context

import java.lang.reflect.Method

/**
 * Creates a bean by calling a `@Bean` method.
 *
 * The first argument is the `@Configuration` bean that declares the method, and the rest are the method's parameters.
 * Throws [[dev.coilix.context.BeanCreationException]] when the method returns `null`.
 *
 * @param method
 *   the `@Bean` method
 */
final case class MethodInstanceFactory(method: Method) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    arguments match {
      case owner :: parameters => produced(method.invoke(owner, parameters*))
      case Nil                 => throw BeanCreationException(s"No owner bean for @Bean method ${method.getName}")
    }

  private def produced(instance: AnyRef): AnyRef =
    Option(instance).getOrElse(throw BeanCreationException(s"@Bean method ${method.getName} returned no instance"))
}
