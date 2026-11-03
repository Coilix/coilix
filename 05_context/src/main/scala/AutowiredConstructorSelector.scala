package dev.coilix.context

import dev.coilix.annotation.Autowired
import java.lang.reflect.Constructor

/**
 * Selects the public constructor annotated with `@Autowired`, or else the public constructor with the most parameters.
 *
 * This is the default [[dev.coilix.context.ConstructorSelector]]. Throws [[dev.coilix.context.NoPublicConstructorException]] for a
 * class without public
 * constructors.
 */
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
