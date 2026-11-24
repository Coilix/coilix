package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Finds the lifecycle methods of a bean while its definition is created. */
trait LifecycleMethodScanner {

  /**
   * The lifecycle methods of `beanClass`.
   *
   * @throws dev.coilix.context.InvalidLifecycleMethodException
   *   if a lifecycle method has parameters, returns a value other than `Unit` or an effect of `Unit`, is static or cannot be made accessible
   */
  def scan(beanName: String, beanClass: Class[?]): LifecycleMethods
}

/** Configuration path and default implementation of [[dev.coilix.context.LifecycleMethodScanner]]. */
object LifecycleMethodScanner {

  /**
   * Replace the component in the configuration under `coilix.context.lifecycle-method-scanner`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[LifecycleMethodScanner] = ConfigPath[LifecycleMethodScanner]("coilix.context.lifecycle-method-scanner")

  /**
   * The configured implementation, or [[dev.coilix.context.AnnotatedLifecycleMethodScanner]] when
   * `coilix.context.lifecycle-method-scanner` is not set.
   */
  def default(using components: ComponentProvider): LifecycleMethodScanner =
    components.get[LifecycleMethodScanner](AnnotatedLifecycleMethodScanner(EffectfulResultRecognizer.default))
}
