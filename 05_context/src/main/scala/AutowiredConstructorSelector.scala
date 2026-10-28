package org.scaling.context

import java.lang.reflect.Constructor
import org.scaling.annotation.Autowired

final class AutowiredConstructorSelector extends ConstructorSelector {

  override def select(beanClass: Class[?]): Constructor[?] =
    autowired(beanClass)
      .orElse(widest(beanClass))
      .getOrElse(throw NoPublicConstructorException(beanClass))

  private def autowired(beanClass: Class[?]): Option[Constructor[?]] =
    beanClass.getConstructors.find(_.isAnnotationPresent(classOf[Autowired]))

  private def widest(beanClass: Class[?]): Option[Constructor[?]] =
    beanClass.getConstructors.maxByOption(_.getParameterCount)
}
