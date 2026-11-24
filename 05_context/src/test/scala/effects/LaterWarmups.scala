package dev.coilix.context
package effects

import dev.coilix.common.GenericType

object LaterWarmups {
  val scanner: AnnotatedLifecycleMethodScanner = AnnotatedLifecycleMethodScanner(TestEffects.recognizer)

  def definition(creation: BeanCreation = BeanCreation.Eager): BeanDefinition =
    BeanDefinition(
      beanName     = "laterWarmup",
      beanType     = GenericType.of(classOf[LaterWarmup]),
      factory      = ConstructorInstanceFactory(classOf[LaterWarmup].getConstructor()),
      dependencies = Nil,
      isPrimary    = false,
      lifecycle    = scanner.scan("laterWarmup", classOf[LaterWarmup]),
      creation     = creation
    )
}
