package org.scaling.context

import java.lang.reflect.Method

final case class MethodInstanceFactory(method: Method) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    arguments match {
      case owner :: parameters => produced(method.invoke(owner, parameters*))
      case Nil                 => throw BeanCreationException(s"No owner bean for @Bean method ${method.getName}")
    }

  private def produced(instance: AnyRef): AnyRef =
    Option(instance).getOrElse(throw BeanCreationException(s"@Bean method ${method.getName} returned no instance"))
}
